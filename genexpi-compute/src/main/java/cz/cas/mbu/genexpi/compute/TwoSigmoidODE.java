package cz.cas.mbu.genexpi.compute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public abstract class TwoSigmoidODE implements FirstOrderDifferentialEquations {

	private final double maxSynthesis0;
	private final double maxSynthesis1;
	private final double bias0;
	private final double bias1;
	private final double decay;
	
	private final double weight0;
	private final double weight1;
		
	private final double initialTime;
	private final double endTime;
	private final double[] regulatorValuesAtInit;
	private final double[] regulatorValuesAtEnd;
	
	private final OneWeightPerRegulatorInferenceTask inferenceTask;
	private final List<UnivariateFunction> regulatorInterpolators;
	
	public TwoSigmoidODE(InferenceModel model, InferenceResult result, OneWeightPerRegulatorInferenceTask inferenceTask,
			List<? extends GeneProfile<?>> geneProfiles, double initialTime, double timeStep) {
		super();
		if(model.getFamily() != InferenceModel.Family.TwoSigmoidRegulation)
		{
			throw new IllegalArgumentException("Invalid model family (" + model.getFamily().name() + ")");
		}
		
		if(Arrays.asList(model.getParameterNames()).contains(InferenceModel.SIGMOID_CONSTITUTIVE_PARAM_NAME)) {
			throw new UnsupportedOperationException("Constitutive expression not supported yet");
		}
		
		if(inferenceTask.getRegulatorIDs().length != 2) {
			throw new UnsupportedOperationException("Only exactly two regulators supported");
		}

		
		this.initialTime = initialTime;
		
		maxSynthesis0 = result.getParameters()[model.getParameterIndex(InferenceModel.SIGMOID_MAX_SYNTH_PARAM_NAME + "_0")];
		maxSynthesis1 = result.getParameters()[model.getParameterIndex(InferenceModel.SIGMOID_MAX_SYNTH_PARAM_NAME + "_1" )];
		bias0 = result.getParameters()[model.getParameterIndex(InferenceModel.SIGMOID_BIAS_PARAM_NAME + "_0")];
		bias1 = result.getParameters()[model.getParameterIndex(InferenceModel.SIGMOID_BIAS_PARAM_NAME + "_1")];
		decay = result.getParameters()[model.getParameterIndex(InferenceModel.SIGMOID_DECAY_PARAM_NAME)];
		
		UnivariateInterpolator interpolator = new LinearInterpolator();
		double[] time = new double[geneProfiles.get(0).getProfile().size()];
		for(int t = 0; t < time.length; t++)
		{
			time[t] = (t * timeStep) + initialTime; 
		}
		this.endTime = time[time.length - 1];
		
		int numRegulators = inferenceTask.getRegulatorIDs().length;
		regulatorInterpolators = new ArrayList<>();
		regulatorValuesAtEnd = new double[numRegulators];
		regulatorValuesAtInit = new double[numRegulators];

		for(int i = 0; i < numRegulators; i++)
		{
			GeneProfile<?> regulatorProfile = geneProfiles.get(inferenceTask.getRegulatorIDs()[i]);
			double[] regulatorValues = new double[regulatorProfile.getProfile().size()];
			for(int t = 0; t < regulatorProfile.getProfile().size(); t++)
			{
				regulatorValues[t] = regulatorProfile.getProfile().get(t).doubleValue();
			}
			regulatorInterpolators.add(interpolator.interpolate(time, regulatorValues));
			regulatorValuesAtInit[i] = regulatorValues[0];
			regulatorValuesAtEnd[i] = regulatorValues[regulatorValues.length - 1];
		}
		
		
		weight0 = result.getParameters()[model.getParameterIndex(InferenceModel.getAdditiveRegulatorWeightParamName(0, numRegulators))];
		weight1 = result.getParameters()[model.getParameterIndex(InferenceModel.getAdditiveRegulatorWeightParamName(1, numRegulators))];
		
		this.inferenceTask = inferenceTask;		
	}

	@Override
	public int getDimension() {
		return 1;
	}
	
	protected double getRegulatorValue(int reg, double t) {
		double interpolatedRegValue;
		if (t < initialTime) {
			interpolatedRegValue = regulatorValuesAtInit[reg];
		} else if (t > endTime) 
		{
			interpolatedRegValue = regulatorValuesAtEnd[reg];
		}
		else
		{
			interpolatedRegValue = regulatorInterpolators.get(reg).value(t);
		}
		return(interpolatedRegValue);
	}

	
	@Override
	public void computeDerivatives(double t, double[] y, double[] yDot)
			throws MaxCountExceededException, DimensionMismatchException {
		yDot[0] = 
				(maxSynthesis0 / (1 + Math.exp(-weight0 * getRegulatorValue(0, t) -bias0))) 
				+ (maxSynthesis1 / (1 + Math.exp(-weight1 * getRegulatorValue(1, t) -bias1))) 
				- decay * y[0]
						;
	}
	
}
