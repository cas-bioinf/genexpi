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
import com.nativelibs4java.opencl.CLPlatform.ContextProperties;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

import com.nativelibs4java.opencl.JavaCL;

/**
 *
 * @author MBU
 */
public class App {
    
	private static class Params 
	{
		File namesFile = null;
		File profilesFile = null;
		File tasksFile = null;
		File outputFile = null;
		File constraintsFile = null;
		InferenceModel model = null;
		int numRegulators = -1;
		EMethod method = EMethod.Annealing;
		EErrorFunction errorFunction = null;
		ELossFunction lossFunction = ELossFunction.Squared;
		int numIterations = 128;	
		float regularizationWeight = 0;
		CLPlatform.DeviceFeature preferredFeature = null;
		CLDevice device = null;
		boolean preventFullOccupation = false;
		boolean useCustomTimeStep = false;
		float customTimeStep = 1.f;
	}
	
    private static <NUMBER_TYPE extends Number> void executeComputation(Class<NUMBER_TYPE> numberType, Params params) throws IOException
    {
         //CLContext context = JavaCL.createBestContext();
    	
    	CLContext context;
    	if(params.device != null)
    	{
			context = params.device.getPlatform().createContext(null, params.device);
    	}
    	else if(params.preferredFeature == null)
    	{
	        context = GNCompute.getBestContext();
    	}else
    	{
    		context = JavaCL.createBestContext(params.preferredFeature, DeviceFeature.MaxComputeUnits, DeviceFeature.OutOfOrderQueueSupport);
    	}
    	
        if(context == null)
        {
        	throw new RuntimeException("No OpenCL context found. You may need to install OpenCL drivers for your GPU/processor.");
        }

        //CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.CPU, CLPlatform.DeviceFeature.OutOfOrderQueueSupport);

        
    	/*
    	CLPlatform platform = JavaCL.listPlatforms()[3]; //OCLGrind 
    	CLContext context = platform.createContext(null, platform.getBestDevice());
    	*/
    	
    	System.out.println(context);
        System.out.println("Model: " + params.model + ", Method: " + params.method + ", Error: " + params.errorFunction + ", Loss: " + params.lossFunction
        		+ ", Iterations: " + params.numIterations + ", Regularization: "	+ params.regularizationWeight);
        System.out.println("Names: " + params.namesFile + ", Profiles: " + params.profilesFile + ", Tasks: " + params.tasksFile + ", Output: " + params.outputFile);        

        List<GeneProfile<NUMBER_TYPE>> profiles = new ArrayList<>();

        List<String> names = Files.readAllLines(params.namesFile.toPath());
        List<String> profilesString = Files.readAllLines(params.profilesFile.toPath());
        
        if(names.size() != profilesString.size())
        {
        	throw new IllegalArgumentException("The size of names and profiles must match");
        }
        
        for(int i = 0; i < names.size();i++)
        {
            String[] numbersString = profilesString.get(i).split(",");
            List<NUMBER_TYPE> profile = new ArrayList<>(numbersString.length);
            for(String s : numbersString)
            {
                if(numberType == Double.class)
                {
                    profile.add((NUMBER_TYPE)new Double(Double.parseDouble(s)));                    
                }
                else if(numberType == Float.class)
                {
                    profile.add((NUMBER_TYPE)new Float(Float.parseFloat(s)));                                        
                }
                else
                {
                    throw new IllegalArgumentException("Must be float or double");
                }
            }
            
            if(i > 0 && profile.size() != profiles.get(i - 1).getProfile().size())
            {
            	throw new IllegalArgumentException("All profiles must have the same length");
            }

            profiles.add(new GeneProfile<>(names.get(i), profile));
        }
        
       
        switch(params.model.getFamily())
        {
	        case AdditiveRegulation:{
	            computeAdditiveRegulation(numberType, params, context, profiles, names);
	            break;
	        }
	        case NoRegulator: {
	        	computeNoRegulator(numberType, params, context, profiles, names);
	        	break;
	        }
	        default:
	        {
	        	throw new IllegalArgumentException("Unrecognized model family: " + params.model.getFamily());
	        }
        }
    }

	private static <NUMBER_TYPE extends Number> void computeAdditiveRegulation(Class<NUMBER_TYPE> numberType,
			Params params, CLContext context, List<GeneProfile<NUMBER_TYPE>> profiles, List<String> names)
			throws IOException {
		int numRegulators = params.numRegulators;
       
        
        List<AdditiveRegulationInferenceTask> inferenceTasks = new ArrayList<>();
        List<Integer> inferenceTasksForOriginalLines = new ArrayList<>(); //keeps track of which tasks corresponds to lines in original file (useful when some tasks cannot be executed and are excluded from the batch).
        //These data simply allow to output even tasks that were not translated to AdditiveRegulationInferenceTask instances 
        List<String> errors = new ArrayList<>();
        List<String> rawTargets = new ArrayList<>();
        List<List<String>> rawRegulators = new ArrayList<>();
                
        List<String> inputLines = Files.readAllLines(params.tasksFile.toPath());
        List<String> constraintLines = null;
        if(params.constraintsFile != null)
        {
        	constraintLines = Files.readAllLines(params.constraintsFile.toPath());
        	if(constraintLines.size() != inputLines.size())
        	{
        		throw new IllegalArgumentException("Number of tasks and constraints does not match");
        	}
        }
        for(int line = 0; line < inputLines.size(); line++)
        {
            String[] fields = inputLines.get(line).split(",");            
            int[] regulatorIDs = new int[numRegulators];
            
            if(fields.length != numRegulators + 1)
            {
            	throw new IllegalArgumentException("Number of regulators at line " + (line + 1) + " (" + fields.length + " regulators) does not match the given number of regulators (" + numRegulators + ") plus one");
            }
            
            String[] weightConstraintsFields = null; 
            RegulationType[] regulationTypes = null;
            if(params.constraintsFile != null)
            {
            	weightConstraintsFields = constraintLines.get(line).split(",");
                regulationTypes = new RegulationType[numRegulators];
                if(weightConstraintsFields.length != numRegulators)
                {
                	throw new IllegalArgumentException("Number of constraints at line " + (line + 1) + " (" + regulationTypes.length + " regulators) does not match the given number of regulators (" + numRegulators + ")");
                }
            }
            
            StringBuilder errorBuilder = new StringBuilder();
            
            List<String> rawRegulatorsThisTask = new ArrayList<>();
            
            boolean regulatorsOK = true;
            for(int regulator = 0; regulator < numRegulators; regulator++)
            {
            	rawRegulatorsThisTask.add(fields[1 + regulator]);
	            int regulatorIndex = names.indexOf(fields[1 + regulator]);
	            if(regulatorIndex < 0)
	            {
	            	String error = "Didn't find regulator " + fields[1] + ". ";
	            	errorBuilder.append(error);
	                regulatorsOK = false;
	            }
	            regulatorIDs[regulator] = regulatorIndex;
	            
	            if(params.constraintsFile != null)
	            {
	            	try {
	            		int intRegulationType = Integer.parseInt(weightConstraintsFields[regulator]);
	            		if(intRegulationType == 1) 
	            		{
	            			regulationTypes[regulator] = RegulationType.PositiveOnly;
	            		}
	            		else if(intRegulationType == -1)
	            		{
	            			regulationTypes[regulator] = RegulationType.NegativeOnly;
	            		}
	            		else if(intRegulationType == 0)
	            		{
	            			regulationTypes[regulator] = RegulationType.All;
	            		}
	            		else
		            	{
		            		throw new IllegalArgumentException("Constraint at line " + (line + 1) + " for regulator " + regulator + " is not +-1 neither 0.");	            		
		            	}
	            	} catch (NumberFormatException nfe)
	            	{
	            		throw new IllegalArgumentException("Constraint at line " + (line + 1) + " for regulator " + regulator + " cannot be parsed as a number");
	            	}
	            }
            }
            
            rawRegulators.add(rawRegulatorsThisTask);
            rawTargets.add(fields[0]);
            
            int targetIndex = names.indexOf(fields[0]);
            if(targetIndex < 0)
            {
            	String error = "Didn't find target " + fields[0] + ". ";
                errorBuilder.append(error);
                inferenceTasksForOriginalLines.add(null);
            }
            
            if(targetIndex > 0 && regulatorsOK)
            {
                inferenceTasksForOriginalLines.add(inferenceTasks.size());
                
            	if(params.constraintsFile != null)
            	{
            		inferenceTasks.add(new AdditiveRegulationInferenceTask(regulatorIDs, targetIndex, regulationTypes));            	
            	}
            	else
            	{
            		inferenceTasks.add(new AdditiveRegulationInferenceTask(regulatorIDs, targetIndex));
            	}
            }
            else
            {
                inferenceTasksForOriginalLines.add(null);            	
            }
            
            if(errorBuilder.length() > 0)
            {
            	errors.add(errorBuilder.toString());
                System.out.println("Task " + (line + 1) + ": " + errorBuilder.toString());
            }
            else
            {
            	errors.add(null);
            }
        }

        GNCompute<NUMBER_TYPE> compute = new GNCompute<>(numberType, context, params.model, params.method, params.errorFunction, params.lossFunction, params.useCustomTimeStep, params.customTimeStep);
               
        List<InferenceResult> results = compute.computeAdditiveRegulation(profiles, inferenceTasks, numRegulators, params.numIterations, params.regularizationWeight, params.preventFullOccupation);
        List<String> outCSVMain = new ArrayList<>();
        
        StringBuilder headerBuilder = new StringBuilder("target");
        if(numRegulators == 1)
        {
        	headerBuilder.append(",reg");
        }
        else
        {
        	for(int i = 0; i < numRegulators; i++)
            {
        		headerBuilder.append(",reg").append(i + 1);
            }        
        }
        
        for(int i = 0; i < params.model.getNumParams(); i++)
        {
        	headerBuilder.append(",").append(params.model.getParameterNames()[i]);        	
        }
        headerBuilder.append(",error");
        outCSVMain.add(headerBuilder.toString());
        
        for(int i = 0; i < inputLines.size(); i++){
            StringBuilder lineBuilder = new StringBuilder();
        	if(inferenceTasksForOriginalLines.get(i) != null)
        	{
        		int taskIndex = inferenceTasksForOriginalLines.get(i);
	            AdditiveRegulationInferenceTask item = inferenceTasks.get(taskIndex);
	            InferenceResult result = results.get(taskIndex);
	            
	            lineBuilder.append(names.get(item.getTargetID()));
	            
	            for(int reg = 0; reg < numRegulators; reg++)
	            {
	            	lineBuilder.append(",").append(names.get(item.getRegulatorIDs()[reg]));            	
	            }
	            
	            for(int p = 0; p < params.model.getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append(result.getParameters()[p]);        	
	            }            
	            lineBuilder.append(",").append(result.getError());
        	}
        	else
        	{
        		lineBuilder.append(rawTargets.get(i));
	            for(int reg = 0; reg < numRegulators; reg++)
	            {
	            	lineBuilder.append(",").append(rawRegulators.get(i).get(reg));            	
	            }
	            
	            for(int p = 0; p < params.model.getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append("NaN");        	
	            }            
	            lineBuilder.append(",").append("NaN");        		
        	}
            outCSVMain.add(lineBuilder.toString());
        }
                
        Files.write(params.outputFile.toPath(), outCSVMain, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
    
	
	private static <NUMBER_TYPE extends Number> void computeNoRegulator(Class<NUMBER_TYPE> numberType,
			Params params, CLContext context, List<GeneProfile<NUMBER_TYPE>> profiles, List<String> names)
			throws IOException {
        
        List<NoRegulatorInferenceTask> inferenceTasks = new ArrayList<>();
        List<Integer> inferenceTasksForOriginalLines = new ArrayList<>(); //keeps track of which tasks corresponds to lines in original file (useful when some tasks cannot be executed and are excluded from the batch).
        //These data simply allow to output even tasks that were not translated to AdditiveRegulationInferenceTask instances 
        List<String> errors = new ArrayList<>();
        List<String> rawTargets = new ArrayList<>();
                
        List<String> inputLines = Files.readAllLines(params.tasksFile.toPath());

        for(int line = 0; line < inputLines.size(); line++)
        {
                        
            StringBuilder errorBuilder = new StringBuilder();
            String targetString = inputLines.get(line);
            rawTargets.add(targetString);
            
            int targetIndex = names.indexOf(targetString);
            if(targetIndex < 0)
            {
            	String error = "Didn't find target '" + line + "'. ";
                errorBuilder.append(error);
                inferenceTasksForOriginalLines.add(null);
            }
            else
            {
                inferenceTasksForOriginalLines.add(inferenceTasks.size());                
        		inferenceTasks.add(new NoRegulatorInferenceTask(targetIndex));
            }
            
            if(errorBuilder.length() > 0)
            {
            	errors.add(errorBuilder.toString());
                System.out.println("Task " + (line + 1) + ": " + errorBuilder.toString());
            }
            else
            {
            	errors.add(null);
            }
        }

        GNCompute<NUMBER_TYPE> compute = new GNCompute<>(numberType, context, params.model, params.method, params.errorFunction, params.lossFunction, params.useCustomTimeStep, params.customTimeStep);
               
        List<InferenceResult> results = compute.computeNoRegulator(profiles, inferenceTasks, params.numIterations, params.preventFullOccupation);
        List<String> outCSVMain = new ArrayList<>();
        
        StringBuilder headerBuilder = new StringBuilder("target");
        
        for(int i = 0; i < params.model.getNumParams(); i++)
        {
        	headerBuilder.append(",").append(params.model.getParameterNames()[i]);        	
        }
        headerBuilder.append(",error");
        outCSVMain.add(headerBuilder.toString());
        
        for(int i = 0; i < inputLines.size(); i++){
            StringBuilder lineBuilder = new StringBuilder();
        	if(inferenceTasksForOriginalLines.get(i) != null)
        	{
        		int taskIndex = inferenceTasksForOriginalLines.get(i);
	            int targetId = inferenceTasks.get(taskIndex).getTargetID();
	            InferenceResult result = results.get(taskIndex);
	            
	            lineBuilder.append(names.get(targetId));
	            	            
	            for(int p = 0; p < params.model.getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append(result.getParameters()[p]);        	
	            }            
	            lineBuilder.append(",").append(result.getError());
        	}
        	else
        	{
        		lineBuilder.append(rawTargets.get(i));
	            
	            for(int p = 0; p < params.model.getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append("NaN");        	
	            }            
	            lineBuilder.append(",").append("NaN");        		
        	}
            outCSVMain.add(lineBuilder.toString());
        }
                
        Files.write(params.outputFile.toPath(), outCSVMain, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
    private static Option numIterationsOption;
    private static Option regularizationWeightOption;
    private static Option cpuOption;
    private static Option gpuOption;
    private static Option listPlatformsOption;
    private static Option deviceIDOption;
    private static Option timeStepOption;
    private static Option preventFullOccupationOption;

    //static initializer for options
    static {
    	options = new Options();
    	outputFileOption = Option.builder("o")
			.hasArg()
			.longOpt("output-file")
			.desc("Use given file for output. If not given a file name is created based on method, model, error and loss functions")
			.build();
    	options.addOption(outputFileOption);
    	
    	modelOption = Option.builder("m")
    			.hasArg()
    			.longOpt("model")
    			.desc("Use given regulatory model. One of [NoRegulator, Additive], default is additive.")
    			.build();
    	options.addOption(modelOption);
		
    	methodOption = Option.builder().longOpt("method")
				.hasArg()
				.desc("Use given optimization method. One of [Annealing]")
				.build();
    	options.addOption(methodOption);
		
    	errorOption = Option.builder("e")
    			.hasArg()
    			.longOpt("error")
    			.desc("Use given error function. For Additive model, it can be one of [Euler, RK4, DerivativeDiff], default is Euler.")
    			.build();
    	options.addOption(errorOption);
    	
    	lossOption = Option.builder("l")
    			.hasArg()
    			.longOpt("loss")
    			.desc("Use given loss function. One of [Abs, Squared], default is Squared")
    			.build();
    	options.addOption(lossOption);
    	
    	numIterationsOption = Option.builder("n")
    			.hasArg()
    			.longOpt("num-iterations")
    			.desc("Use given number of iterations, default is 256")
    			.build();
    	options.addOption(numIterationsOption);
    	regularizationWeightOption = Option.builder("r")
    			.hasArg()
    			.longOpt("regularization")
    			.desc("The weight of regularization, default is 0.")
    			.build();
    	options.addOption(regularizationWeightOption);
    	
    	numRegulatorsOption = Option.builder("g")
    			.hasArg()
    			.longOpt("num-regulators")
    			.desc("Number of regulators (relevant only for the Additive model).")
    			.build();
    	options.addOption(numRegulatorsOption);
    	constraintsFileOption = Option.builder("w")
    			.hasArg()
    			.longOpt("weight-constraints")
    			.desc("Optional file containing weight constraints (+/-1 to restrict to positive/negative regulations or 0 for no constraints) for each regulator in each task. \nApplicable only for the Additive model.")
    			.build();
    	options.addOption(constraintsFileOption);
    	
    	useConstitutiveOption = Option.builder("c")
    			.longOpt("constitutive")
    			.desc("Use constitutive expression in the model (relevant only for the Additive model)")
    			.build();
    	options.addOption(useConstitutiveOption);
    	
    	cpuOption = Option.builder("cpu")
    			.desc("Prefer CPU OpenCL platforms")
    			.build();
    	options.addOption(cpuOption);
    	gpuOption = Option.builder("gpu")
    			.desc("Prefer GPU OpenCL platforms")
    			.build();
    	options.addOption(gpuOption);
    	
    	listPlatformsOption = Option.builder("ldev")
    			.longOpt("list-devices")
    			.desc("List all available OpenCL devices")
    			.build();
    	options.addOption(listPlatformsOption);
    	
    	deviceIDOption = Option.builder("dev")
    			.longOpt("device")
    			.hasArg()
    			.desc("ID of OpenCL device to use (as returned by --list-devices)")
    			.build();
    	options.addOption(deviceIDOption);

    	timeStepOption = Option.builder("step")
    			.longOpt("time-step")
    			.hasArg()
    			.desc("Time step between the individual time points (default is 1).")
    			.build();
    	options.addOption(timeStepOption);
    	
    	preventFullOccupationOption = Option.builder("pfo")
    			.longOpt("prevent-full-occupation")
    			.desc("Prevent full occupation of the OpenCL device. Keeps the computer responsive when computing on the GPU that runs the main display.")
    			.build();   
    	options.addOption(preventFullOccupationOption);
    	
    }
    
    private static void inputError(String msg)
    {
    	System.out.println(msg);
		printHelp();
		System.exit(1);
    }
    
    private static void printHelp()
    {
    	HelpFormatter formatter = new HelpFormatter();
    	formatter.printHelp("java -jar genexpi-standalone.jar names_file profiles_file tasks_file -m <model> [OPTIONS]", options);
    	System.out.println("For further information, visit https://github.com/cas-bioinf/genexpi/wiki/Command-line");
    }
    
    public static void main(String[] args) throws IOException {
    	
		CommandLineParser parser = new DefaultParser();
    	Params params = new Params();
		try 
		{
			CommandLine line = parser.parse(options, args);
			
			if(line.hasOption(listPlatformsOption.getOpt()))
			{
				System.out.println("Listing available OpenCL platforms:");
				int id = 0; 				
				for(CLPlatform platform : JavaCL.listPlatforms())					
				{
					for(CLDevice device: platform.listAllDevices(false))
					{
						System.out.println("["+id+"]\t" + platform.getName() + ": " + device.getName());
						id++;
					}						
				}
				return;
			}
			
			if(line.getArgList().size() != 3)
			{
				inputError("You have to specify three files for processing");				
			}
			params.namesFile = new File(line.getArgList().get(0));
			params.profilesFile = new File(line.getArgList().get(1));
			params.tasksFile = new File(line.getArgList().get(2));

			String outputFileValue = line.getOptionValue(outputFileOption.getOpt());
			if( outputFileValue != null)
			{
				params.outputFile = new File(outputFileValue);
			}

			String weightConstraintsFileValue = line.getOptionValue(constraintsFileOption.getOpt());
			if( weightConstraintsFileValue != null)
			{
				params.constraintsFile = new File(weightConstraintsFileValue);
			}
			
			String numIterationsValue = line.getOptionValue(numIterationsOption.getOpt());
			if(numIterationsValue != null)
			{
				try {
					params.numIterations = Integer.parseInt(numIterationsValue);
				}
				catch(NumberFormatException ex)
				{
					inputError("Number of iterations must be an integer.");
				}
			}
			
			String modelName = line.getOptionValue(modelOption.getOpt());
			if(modelName == null || modelName.equals("Additive"))
			{
				String numRegulatorsValue = line.getOptionValue(numRegulatorsOption.getOpt());
				if(numRegulatorsValue == null)
				{
					inputError("You have to specify the number of regulators for the additive model (e.g.: -g 1).");
				}
				else
				{
					try {
						params.numRegulators = Integer.parseInt(numRegulatorsValue);
						boolean useConstitutive = (line.hasOption(useConstitutiveOption.getOpt()));
						params.model = InferenceModel.createAdditiveRegulationModel(params.numRegulators, useConstitutive);						
					}
					catch(NumberFormatException ex)
					{
						inputError("Number of regulators must be an integer.");
					}				
				}
				
				String errorValue = line.getOptionValue(errorOption.getOpt());
				if(errorValue != null)
				{
					try {
						params.errorFunction = EErrorFunction.valueOf(errorValue);
					} catch (IllegalArgumentException ex)
					{
						inputError("Invalid error function specification");
					}				
				}
				else 
				{
					params.errorFunction = EErrorFunction.Euler;
				}
				
			}
			else if (modelName.equals("NoRegulator"))
			{
				params.model = InferenceModel.NO_REGULATOR;
			}
			else
			{
				inputError("Invalid model specification: '" + line.getOptionValue(modelOption.getOpt()) + "'");
			}
				
			
			String lossValue = line.getOptionValue(lossOption.getOpt());
			if(lossValue != null)
			{
				try {
					params.lossFunction = ELossFunction.valueOf(lossValue);
				} catch (IllegalArgumentException ex)
				{
					inputError("Invalid loss function specification");
				}				
			}		
			
			String regularizationValue = line.getOptionValue(regularizationWeightOption.getOpt());
			if(regularizationValue != null)
			{
				try {
					params.regularizationWeight = Float.parseFloat(regularizationValue);
				}
				catch(NumberFormatException ex)
				{
					inputError("Regularization weight must be a floating-point number.");
				}
			}			
			
			if(line.hasOption(timeStepOption.getOpt()))
			{
				String timeStepValue = line.getOptionValue(timeStepOption.getOpt());
				params.useCustomTimeStep = true;
				try {
					params.customTimeStep = Float.parseFloat(timeStepValue);
				}
				catch(NumberFormatException ex)
				{
					inputError("Time step must be a floating-point number.");
				}
				
			}
			
			if(line.hasOption(deviceIDOption.getOpt()))
			{
				int platformID = Integer.parseInt(line.getOptionValue(deviceIDOption.getOpt()));
				int currentID = 0;
				boolean found = false;
				outer: for(CLPlatform platform : JavaCL.listPlatforms())					
				{
					for(CLDevice device: platform.listAllDevices(false))
					{
						if(currentID == platformID)
						{
							found = true;
							params.device = device;
							break outer;
						}
						currentID++;
					}						
				}
				
				if(!found)
				{
					inputError("There are " + currentID + " devices. The device ID has to lie between 0 and " + (currentID - 1) + "." );;
				}
			}
			else if(line.hasOption(cpuOption.getOpt()))
			{
				params.preferredFeature = DeviceFeature.CPU;
			}
			else if(line.hasOption(gpuOption.getOpt()))
			{
				params.preferredFeature = DeviceFeature.GPU;
			}
		
			if (line.hasOption(preventFullOccupationOption.getOpt()))
			{
				params.preventFullOccupation = true;
			}
			else
			{
				params.preventFullOccupation = false;				
			}
		}
		catch(ParseException ex)
		{
			inputError(ex.getMessage());
		}
		
    			
    	
    	if(params.outputFile == null)
    	{
    		params.outputFile = new File(params.model.toString() + "_" + params.method.name() + "_results_" + params.errorFunction.name() + "_" + params.lossFunction.name() + ".csv");    		
    	}    	
    	
    	try {
    		executeComputation(Float.class, params);
    	} catch (SuspectGPUResetByOSException ex)
    	{
    		ex.printStackTrace();
        	System.out.println(ex.getMessage());
        	System.out.println("Consider using another OpenCL device (see --" + deviceIDOption.getLongOpt() + " and -" + cpuOption.getOpt() + " options)"
        			+ "or use -" + preventFullOccupationOption.getOpt() + ".");
    		System.exit(1);
    		
    	}
    }    
}
