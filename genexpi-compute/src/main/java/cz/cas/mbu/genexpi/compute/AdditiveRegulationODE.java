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

public class AdditiveRegulationODE implements FirstOrderDifferentialEquations {

	private final double maxSynthesis;
	private final double decay;
	private final double bias;
	private final double[] weights;
	
	private final double initialTime;
	private final double endTime;
	private final double[] regulatorValuesAtInit;
	private final double[] regulatorValuesAtEnd;
	
	private final AdditiveRegulationInferenceTask inferenceTask;
	private final List<UnivariateFunction> regulatorInterpolators;
	
	public AdditiveRegulationODE(InferenceModel model, InferenceResult result, AdditiveRegulationInferenceTask inferenceTask,
			List<? extends GeneProfile<?>> geneProfiles, double initialTime, double timeStep) {
		super();
		if(model.getFamily() != InferenceModel.Family.AdditiveRegulation)
		{
			throw new IllegalArgumentException("Invalid model family (" + model.getFamily().name() + ") - expected " + InferenceModel.Family.AdditiveRegulation.name());
		}
		
		this.initialTime = initialTime;
		
		maxSynthesis = result.getParameters()[model.getParameterIndex(InferenceModel.ADDITIVE_MAX_SYNTH_PARAM_NAME)];
		decay = result.getParameters()[model.getParameterIndex(InferenceModel.ADDITIVE_DECAY_PARAM_NAME)];
		bias = result.getParameters()[model.getParameterIndex(InferenceModel.ADDITIVE_BIAS_PARAM_NAME)];
		
		UnivariateInterpolator interpolator = new LinearInterpolator();
		double[] time = new double[geneProfiles.get(0).getProfile().size()];
		for(int t = 0; t < time.length; t++)
		{
			time[t] = (t * timeStep) + initialTime; 
		}
		this.endTime = time[time.length - 1];
		
		int numRegulators = inferenceTask.getRegulatorIDs().length;
		weights = new double[numRegulators];
		regulatorInterpolators = new ArrayList<>();
		regulatorValuesAtEnd = new double[numRegulators];
		regulatorValuesAtInit = new double[numRegulators];

		for(int i = 0; i < numRegulators; i++)
		{
			weights[i] = result.getParameters()[model.getParameterIndex(InferenceModel.getAdditiveRegulatorWeightParamName(i, numRegulators))];
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
		
		this.inferenceTask = inferenceTask;		
	}

	@Override
	public int getDimension() {
		return 1;
	}

	@Override
	public void computeDerivatives(double t, double[] y, double[] yDot)
			throws MaxCountExceededException, DimensionMismatchException {
		double regulatoryInput = 0;
		for(int reg = 0; reg < weights.length; reg++)
		{
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
			regulatoryInput += weights[reg] * Math.max(0, interpolatedRegValue); //ensure regulatory input above zero
		}
		yDot[0] = (maxSynthesis / (1 + Math.exp(-regulatoryInput -bias))) - decay * y[0];
	}
	
}
