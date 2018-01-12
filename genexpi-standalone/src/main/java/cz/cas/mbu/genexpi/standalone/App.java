/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.BaseInferenceEngine;
import cz.cas.mbu.genexpi.compute.ComputeUtils;
import cz.cas.mbu.genexpi.compute.CooperativeRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.IInferenceEngine;
import cz.cas.mbu.genexpi.compute.InferenceEngineBuilder;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

/**
 *
 * @author MBU
 */
public class App {

	private static class Params {
		File namesFile = null;
		File profilesFile = null;
		File tasksFile = null;
		File outputFile = null;
		File constraintsFile = null;
		boolean verbose = false;
		int numRegulators = -1;
		InferenceModel.Family modelFamily = null;
		EMethod method = EMethod.Annealing;
		EErrorFunction errorFunction = null;
		ELossFunction lossFunction = ELossFunction.Squared;
		int numIterations = 128;
		float regularizationWeight = -1;
		CLPlatform.DeviceFeature preferredFeature = null;
		CLDevice device = null;
		boolean preventFullOccupation = false;
		boolean useCustomTimeStep = false;
		float customTimeStep = 1.f;
		boolean useConstitutiveExpression = false;

		public <NUMBER_TYPE extends Number> InferenceEngineBuilder<NUMBER_TYPE> getEngineBuilder(
				Class<NUMBER_TYPE> numberType) {
			return (new InferenceEngineBuilder<>(numberType).setMethod(method).setErrorFunction(errorFunction)
					.setLossFunction(lossFunction).setUseCustomTimeStep(useCustomTimeStep)
					.setCustomTimeStep(customTimeStep).setNumIterations(numIterations)
					.setPreventFullOccupation(preventFullOccupation))
					.setVerbose(verbose);
		}
	}
	
	private static void printComputationParams(Params params, IInferenceEngine<?,?> inferenceEngine) {
		if(params.verbose) {
			System.out.println("Model: " + inferenceEngine.getModel() + ", Method: " + params.method + ", Error: " + params.errorFunction
					+ ", Loss: " + params.lossFunction + ", Iterations: " + params.numIterations + ", Regularization: "
					+ params.regularizationWeight);
		}
	}

	private static <NUMBER_TYPE extends Number> void executeComputation(Class<NUMBER_TYPE> numberType, Params params)
			throws IOException {
		// CLContext context = JavaCL.createBestContext();

		CLContext context;
		if (params.device != null) {
			context = params.device.getPlatform().createContext(null, params.device);
		} else if (params.preferredFeature == null) {
			context = ComputeUtils.getBestContext();
		} else {
			context = JavaCL.createBestContext(params.preferredFeature, DeviceFeature.MaxComputeUnits,
					DeviceFeature.OutOfOrderQueueSupport);
		}

		if (context == null) {
			throw new RuntimeException(
					"No OpenCL context found. You may need to install OpenCL drivers for your GPU/processor.");
		}

		if(params.verbose) {
			System.out.println("Names: " + params.namesFile + ", Profiles: " + params.profilesFile + ", Tasks: "
					+ params.tasksFile + ", Output: " + params.outputFile);
			System.out.println(context);
		}

		List<GeneProfile<NUMBER_TYPE>> profiles = new ArrayList<>();

		List<String> names = Files.readAllLines(params.namesFile.toPath());
		List<String> profilesString = Files.readAllLines(params.profilesFile.toPath());

		if (names.size() != profilesString.size()) {
			throw new IllegalArgumentException("The size of names and profiles must match");
		}

		for (int i = 0; i < names.size(); i++) {
			String[] numbersString = profilesString.get(i).split(",");
			List<NUMBER_TYPE> profile = new ArrayList<>(numbersString.length);
			for (String s : numbersString) {
				if (numberType == Double.class) {
					profile.add((NUMBER_TYPE) new Double(Double.parseDouble(s)));
				} else if (numberType == Float.class) {
					profile.add((NUMBER_TYPE) new Float(Float.parseFloat(s)));
				} else {
					throw new IllegalArgumentException("Must be float or double");
				}
			}

			if (i > 0 && profile.size() != profiles.get(i - 1).getProfile().size()) {
				throw new IllegalArgumentException("All profiles must have the same length");
			}

			profiles.add(new GeneProfile<>(names.get(i), profile));
		}

		switch (params.modelFamily) {
			case AdditiveRegulation: {
				guessRegularizationWeightIfNecessary(params, profiles);
				computeAdditiveRegulation(numberType, params, context, profiles, names);
				break;
			}
			case CooperativeRegulation: {
				guessRegularizationWeightIfNecessary(params, profiles);
				computeCooperativeRegulation(numberType, params, context, profiles, names);
				break;
			}
			case NoRegulator: {
				computeNoRegulator(numberType, params, context, profiles, names);
				break;
			}
			default: {
				throw new IllegalArgumentException("Unrecognized model family: " + params.modelFamily);
			}
		}
	}

	private static <NUMBER_TYPE extends Number> void guessRegularizationWeightIfNecessary(Params params, List<GeneProfile<NUMBER_TYPE>> profiles) {
		if(params.regularizationWeight < 0) {
			params.regularizationWeight = (float)profiles.get(0).getProfile().size() / 10.0f;
			if(params.verbose) {
				System.out.println("Guessing regularization weight to be: " + params.regularizationWeight);
			}
		}
		
	}

	private static <NUMBER_TYPE extends Number> void computeAdditiveRegulation(Class<NUMBER_TYPE> numberType,
			Params params, CLContext context, List<GeneProfile<NUMBER_TYPE>> profiles, List<String> names)
			throws IOException {
		int numRegulators = params.numRegulators;
       
        TasksReadResult<AdditiveRegulationInferenceTask> taskReadResult = new AdditiveTasksReader(numRegulators).readTasks(names, params.tasksFile, params.constraintsFile);
        
        List<AdditiveRegulationInferenceTask> inferenceTasks = taskReadResult.getInferenceTasks();      

        IInferenceEngine<NUMBER_TYPE, AdditiveRegulationInferenceTask> compute = params.getEngineBuilder(numberType)
        		.setContext(context)
        		.buildAdditiveRegulation(numRegulators, params.useConstitutiveExpression, params.regularizationWeight);
        		
        		              
        printComputationParams(params, compute);
        List<InferenceResult> results = compute.compute(profiles, inferenceTasks);

        new AdditiveResultsWriter(numRegulators).write(names, taskReadResult, compute, results, params.outputFile);
	}	
	
	private static <NUMBER_TYPE extends Number> void computeCooperativeRegulation(Class<NUMBER_TYPE> numberType,
			Params params, CLContext context, List<GeneProfile<NUMBER_TYPE>> profiles, List<String> names)
			throws IOException {
       
        TasksReadResult<CooperativeRegulationInferenceTask> taskReadResult = new CooperativeTasksReader().readTasks(names, params.tasksFile, params.constraintsFile);
        
        List<CooperativeRegulationInferenceTask> inferenceTasks = taskReadResult.getInferenceTasks();      

        IInferenceEngine<NUMBER_TYPE, CooperativeRegulationInferenceTask> compute = params.getEngineBuilder(numberType)
        		.setContext(context)
        		.buildCooperativeRegulation(params.useConstitutiveExpression, params.regularizationWeight);
        		
        		              
        printComputationParams(params, compute);
        List<InferenceResult> results = compute.compute(profiles, inferenceTasks);

        new CooperativeResultsWriter().write(names, taskReadResult, compute, results, params.outputFile);
	}		
	
	private static <NUMBER_TYPE extends Number> void computeNoRegulator(Class<NUMBER_TYPE> numberType, Params params,
			CLContext context, List<GeneProfile<NUMBER_TYPE>> profiles, List<String> names) throws IOException {

        TasksReadResult<NoRegulatorInferenceTask> taskReadResult = new NoRegulatorTasksReader().readTasks(names, params.tasksFile, params.constraintsFile);
		
		List<NoRegulatorInferenceTask> inferenceTasks = taskReadResult.getInferenceTasks();

		IInferenceEngine<NUMBER_TYPE, NoRegulatorInferenceTask> compute = 
				params.getEngineBuilder(numberType)
				.setContext(context).buildNoRegulator();
						
        printComputationParams(params, compute);
		
		List<InferenceResult> results = compute.compute(profiles, inferenceTasks);
		
        new NoRegulatorResultsWriter().write(names, taskReadResult, compute, results, params.outputFile);		
	}

	private static Options options;
	private static Option outputFileOption;
	private static Option constraintsFileOption;
	private static Option modelOption;
	private static Option numRegulatorsOption;
	private static Option useConstitutiveOption;
	private static Option methodOption;
	private static Option errorOption;
	private static Option lossOption;
	private static Option precisionOption;
	private static Option numIterationsOption;
	private static Option regularizationWeightOption;
	private static Option cpuOption;
	private static Option gpuOption;
	private static Option listPlatformsOption;
	private static Option deviceIDOption;
	private static Option timeStepOption;
	private static Option preventFullOccupationOption;
	private static Option verboseOption;

	// static initializer for options
	static {
		options = new Options();
		outputFileOption = Option.builder("o").hasArg().longOpt("output-file")
				.desc("Use given file for output. If not given a file name is created based on method, model, error and loss functions")
				.build();
		options.addOption(outputFileOption);

		modelOption = Option.builder("m").hasArg().longOpt("model")
				.desc("Use given regulatory model. One of [NoRegulator, Additive], default is additive.").build();
		options.addOption(modelOption);

		methodOption = Option.builder().longOpt("method").hasArg()
				.desc("Use given optimization method. One of [Annealing]").build();
		options.addOption(methodOption);

		errorOption = Option.builder("e").hasArg().longOpt("error")
				.desc("Use given error function. For Additive model, it can be one of [Euler, RK4, DerivativeDiff], default is Euler.")
				.build();
		options.addOption(errorOption);

		lossOption = Option.builder("l").hasArg().longOpt("loss")
				.desc("Use given loss function. One of [Abs, Squared], default is Squared").build();
		options.addOption(lossOption);

		precisionOption = Option.builder("p").hasArg().longOpt("precision")
				.desc("Use a given precision (single or double) for the computation. Default is single.")
				.build();
		options.addOption(precisionOption);
		
		numIterationsOption = Option.builder("n").hasArg().longOpt("num-iterations")
				.desc("Use given number of iterations, default is 256").build();
		options.addOption(numIterationsOption);
		regularizationWeightOption = Option.builder("r").hasArg().longOpt("regularization")
				.desc("The weight of regularization, default is 0.").build();
		options.addOption(regularizationWeightOption);

		numRegulatorsOption = Option.builder("g").hasArg().longOpt("num-regulators")
				.desc("Number of regulators (relevant only for the Additive model).").build();
		options.addOption(numRegulatorsOption);
		constraintsFileOption = Option.builder("w").hasArg().longOpt("weight-constraints")
				.desc("Optional file containing weight constraints (+/-1 to restrict to positive/negative regulations or 0 for no constraints) for each regulator in each task. \nApplicable only for the Additive model.")
				.build();
		options.addOption(constraintsFileOption);

		useConstitutiveOption = Option.builder("c").longOpt("constitutive")
				.desc("Use constitutive expression in the model (relevant only for the Additive model)").build();
		options.addOption(useConstitutiveOption);

		cpuOption = Option.builder("cpu").desc("Prefer CPU OpenCL platforms").build();
		options.addOption(cpuOption);
		gpuOption = Option.builder("gpu").desc("Prefer GPU OpenCL platforms").build();
		options.addOption(gpuOption);

		listPlatformsOption = Option.builder("ldev").longOpt("list-devices").desc("List all available OpenCL devices")
				.build();
		options.addOption(listPlatformsOption);

		deviceIDOption = Option.builder("dev").longOpt("device").hasArg()
				.desc("ID of OpenCL device to use (as returned by --list-devices)").build();
		options.addOption(deviceIDOption);

		timeStepOption = Option.builder("step").longOpt("time-step").hasArg()
				.desc("Time step between the individual time points (default is 1).").build();
		options.addOption(timeStepOption);

		preventFullOccupationOption = Option.builder("pfo").longOpt("prevent-full-occupation")
				.desc("Prevent full occupation of the OpenCL device. Keeps the computer responsive when computing on the GPU that runs the main display.")
				.build();
		options.addOption(preventFullOccupationOption);

		verboseOption = Option.builder("v").longOpt("verbose").desc("Show information about the computation taking place.").build();
		options.addOption(verboseOption);
	}

	private static void inputError(String msg) {
		System.out.println(msg);
		printHelp();
		System.exit(1);
	}

	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar genexpi-standalone.jar names_file profiles_file tasks_file -m <model> [OPTIONS]",
				options);
		System.out.println("For further information, visit https://github.com/cas-bioinf/genexpi/wiki/Command-line");
	}

	public static void main(String[] args) throws IOException {

		CommandLineParser parser = new DefaultParser();
		Params params = new Params();
		boolean useDouble = false;
		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(listPlatformsOption.getOpt())) {
				System.out.println("Listing available OpenCL devices:");
				int id = 0;
				for (CLPlatform platform : JavaCL.listPlatforms()) {
					for (CLDevice device : platform.listAllDevices(false)) {
						System.out.println("[" + id + "]\t" + platform.getName() + ": " + device.getName());
						id++;
					}
				}
				return;
			}

			if (line.getArgList().size() != 3) {
				inputError("You have to specify three files for processing");
			}
			params.namesFile = new File(line.getArgList().get(0));
			params.profilesFile = new File(line.getArgList().get(1));
			params.tasksFile = new File(line.getArgList().get(2));

			String outputFileValue = line.getOptionValue(outputFileOption.getOpt());
			if (outputFileValue != null) {
				params.outputFile = new File(outputFileValue);
			}

			String weightConstraintsFileValue = line.getOptionValue(constraintsFileOption.getOpt());
			if (weightConstraintsFileValue != null) {
				params.constraintsFile = new File(weightConstraintsFileValue);
			}

			String numIterationsValue = line.getOptionValue(numIterationsOption.getOpt());
			if (numIterationsValue != null) {
				try {
					params.numIterations = Integer.parseInt(numIterationsValue);
				} catch (NumberFormatException ex) {
					inputError("Number of iterations must be an integer.");
				}
			}

			String modelName = line.getOptionValue(modelOption.getOpt());
			if (modelName == null || modelName.equals("Additive")) {
				String numRegulatorsValue = line.getOptionValue(numRegulatorsOption.getOpt());
				if (numRegulatorsValue == null) {
					inputError("You have to specify the number of regulators for the additive model (e.g.: -g 1).");
				} else {
					try {
						params.numRegulators = Integer.parseInt(numRegulatorsValue);
						params.useConstitutiveExpression = (line.hasOption(useConstitutiveOption.getOpt()));
						params.modelFamily = InferenceModel.Family.AdditiveRegulation;
					} catch (NumberFormatException ex) {
						inputError("Number of regulators must be an integer.");
					}
				}
				
			} else if (modelName.equals("Cooperative")) {
				String numRegulatorsValue = line.getOptionValue(numRegulatorsOption.getOpt());
				if(numRegulatorsValue != null && !numRegulatorsValue.trim().equals("2")) {
					inputError("Cooperative model currently supports only exactly two regulators");
				}
				params.useConstitutiveExpression = (line.hasOption(useConstitutiveOption.getOpt()));
				params.modelFamily = InferenceModel.Family.CooperativeRegulation;
			} else if (modelName.equals("NoRegulator")) {
				params.modelFamily = InferenceModel.Family.NoRegulator;
			} else {
				inputError("Invalid model specification: '" + line.getOptionValue(modelOption.getOpt()) + "'");
			}

			String errorValue = line.getOptionValue(errorOption.getOpt());
			if (errorValue != null) {
				try {
					params.errorFunction = EErrorFunction.valueOf(errorValue);
				} catch (IllegalArgumentException ex) {
					inputError("Invalid error function specification");
				}
			} else {
				params.errorFunction = EErrorFunction.Euler;
			}

			String lossValue = line.getOptionValue(lossOption.getOpt());
			if (lossValue != null) {
				try {
					params.lossFunction = ELossFunction.valueOf(lossValue);
				} catch (IllegalArgumentException ex) {
					inputError("Invalid loss function specification");
				}
			}

			String regularizationValue = line.getOptionValue(regularizationWeightOption.getOpt());
			if (regularizationValue != null) {
				try {
					params.regularizationWeight = Float.parseFloat(regularizationValue);
				} catch (NumberFormatException ex) {
					inputError("Regularization weight must be a floating-point number.");
				}
			}

			if (line.hasOption(timeStepOption.getOpt())) {
				String timeStepValue = line.getOptionValue(timeStepOption.getOpt());
				params.useCustomTimeStep = true;
				try {
					params.customTimeStep = Float.parseFloat(timeStepValue);
				} catch (NumberFormatException ex) {
					inputError("Time step must be a floating-point number.");
				}

			}

			if (line.hasOption(deviceIDOption.getOpt())) {
				int platformID = Integer.parseInt(line.getOptionValue(deviceIDOption.getOpt()));
				int currentID = 0;
				boolean found = false;
				outer: for (CLPlatform platform : JavaCL.listPlatforms()) {
					for (CLDevice device : platform.listAllDevices(false)) {
						if (currentID == platformID) {
							found = true;
							params.device = device;
							break outer;
						}
						currentID++;
					}
				}

				if (!found) {
					inputError("There are " + currentID + " devices. The device ID has to lie between 0 and "
							+ (currentID - 1) + ".");
					;
				}
			} else if (line.hasOption(cpuOption.getOpt())) {
				params.preferredFeature = DeviceFeature.CPU;
			} else if (line.hasOption(gpuOption.getOpt())) {
				params.preferredFeature = DeviceFeature.GPU;
			}

			if (line.hasOption(preventFullOccupationOption.getOpt())) {
				params.preventFullOccupation = true;
			} else {
				params.preventFullOccupation = false;
			}
			
			if(line.hasOption(verboseOption.getOpt())){
				params.verbose = true;
			}
			
			String precisionValue = line.getOptionValue(precisionOption.getOpt());
			if(precisionValue == null || precisionValue.equals("single")) {				
				useDouble = false;
			} else if (precisionValue.equals("double")) {
				useDouble = true;
			} else {
				inputError("Precision has to be either 'single' or 'double'.");
			}
		} catch (ParseException ex) {
			inputError(ex.getMessage());
		}

		if (params.outputFile == null) {
			String errFuncStr = "";
			if (params.errorFunction != null) {
				errFuncStr = params.errorFunction.name() + "_";
			}
			params.outputFile = new File(params.modelFamily.toString() + "_" + params.method.name() + "_results_" + errFuncStr
					+ params.lossFunction.name() + ".csv");
		}

		try {
			if(useDouble) {				
				executeComputation(Double.class, params);
			} else {
				executeComputation(Float.class, params);
			}
		} catch (SuspectGPUResetByOSException ex) {
			ex.printStackTrace();
			System.out.println(ex.getMessage());
			System.out.println("Consider using another OpenCL device (see --" + deviceIDOption.getLongOpt() + " and -"
					+ cpuOption.getOpt() + " options)" + "or use -" + preventFullOccupationOption.getOpt() + ".");
			System.exit(1);

		}
	}
}
