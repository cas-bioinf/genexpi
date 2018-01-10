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

public class AdditiveRegulationODE extends BaseSigmoidODE {

	private final double[] weights;
		
	public AdditiveRegulationODE(InferenceModel model, InferenceResult result, AdditiveRegulationInferenceTask inferenceTask,
			List<? extends GeneProfile<?>> geneProfiles, double initialTime, double timeStep) {
		super(model, result, inferenceTask, geneProfiles, initialTime, timeStep);
		if(model.getFamily() != InferenceModel.Family.AdditiveRegulation)
		{
			throw new IllegalArgumentException("Invalid model family (" + model.getFamily().name() + ") - expected " + InferenceModel.Family.AdditiveRegulation.name());
		}
		

		int numRegulators = inferenceTask.getRegulatorIDs().length;
		
		weights = new double[numRegulators];

		for(int i = 0; i < numRegulators; i++)
		{
			weights[i] = result.getParameters()[model.getParameterIndex(InferenceModel.getAdditiveRegulatorWeightParamName(i, numRegulators))];
		}		
	}

	@Override
	public int getDimension() {
		return 1;
	}

	
	
	@Override
	protected double computeRegulatoryInput(double t) {
		// TODO Auto-generated method stub
		double regulatoryInput = 0;
		for(int reg = 0; reg < weights.length; reg++)
		{
			double interpolatedRegValue = getRegulatorValue(reg, t);
			regulatoryInput += weights[reg] * Math.max(0, interpolatedRegValue); //ensure regulatory input above zero
		}
		return regulatoryInput;
	}	
}
