package cz.cas.mbu.genexpi.compute;

import java.io.IOException;

import com.nativelibs4java.opencl.CLContext;

public class InferenceEngineBuilder<NUMBER_TYPE extends Number> {
	protected final Class<NUMBER_TYPE> elementClass;
	protected CLContext context;
	protected EMethod method = EMethod.Annealing;
	protected EErrorFunction errorFunction;
	protected ELossFunction lossFunction = ELossFunction.Squared;

	protected boolean useCustomTimeStep = false;
	protected float customTimeStep = Float.NaN;

	protected boolean verbose = false;
	protected int numIterations = 256;
	protected boolean preventFullOccupation = false;

		
	public InferenceEngineBuilder(Class<NUMBER_TYPE> elementClass) {
		this.elementClass = elementClass;
	}

	public IInferenceEngine<NUMBER_TYPE, NoRegulatorInferenceTask> buildNoRegulator() throws IOException {
		return new NoRegulatorInferenceEngine<>(elementClass, context, method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose, numIterations, preventFullOccupation);
	}
		
	public IInferenceEngine<NUMBER_TYPE, AdditiveRegulationInferenceTask> buildAdditiveRegulation(int numRegulators, boolean useConstitutiveExpression, float regularizationWeight) throws IOException {
		return new AdditiveRegulationInferenceEngine<>(elementClass, context, method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose, numIterations, preventFullOccupation, numRegulators, regularizationWeight, useConstitutiveExpression);
	}

	public IInferenceEngine<NUMBER_TYPE, CooperativeRegulationInferenceTask> buildCooperativeRegulation(boolean useConstitutiveExpression, float regularizationWeight) throws IOException {
		return new CooperativeRegulationInferenceEngine<>(elementClass, context, method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose, numIterations, preventFullOccupation, regularizationWeight, useConstitutiveExpression);
	}
		
	public InferenceEngineBuilder<NUMBER_TYPE> setContext(CLContext context) {
		this.context = context;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setMethod(EMethod method) {
		this.method = method;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setErrorFunction(EErrorFunction errorFunction) {
		this.errorFunction = errorFunction;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setLossFunction(ELossFunction lossFunction) {
		this.lossFunction = lossFunction;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setUseCustomTimeStep(boolean useCustomTimeStep) {
		this.useCustomTimeStep = useCustomTimeStep;
		return (this);
	}
	
	public InferenceEngineBuilder<NUMBER_TYPE> setCustomTimeStep(float customTimeStep) {
		this.customTimeStep = customTimeStep;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setVerbose(boolean verbose) {
		this.verbose = verbose;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setNumIterations(int numIterations) {
		this.numIterations = numIterations;
		return (this);
	}

	public InferenceEngineBuilder<NUMBER_TYPE> setPreventFullOccupation(boolean preventFullOccupation) {
		this.preventFullOccupation = preventFullOccupation;
		return (this);
	}

}
