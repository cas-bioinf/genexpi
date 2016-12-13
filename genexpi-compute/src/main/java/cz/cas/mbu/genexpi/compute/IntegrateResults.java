package cz.cas.mbu.genexpi.compute;

import java.util.List;

import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;

public class IntegrateResults {
	public static double[] integrateAdditiveRegulation(InferenceModel model, InferenceResult inferenceResult, 
			AdditiveRegulationInferenceTask inferenceTask, List<? extends GeneProfile<?>> geneProfiles, double profileInitialTime, double profileTimeStep, double[] targetTimePoints)
	{
		FirstOrderDifferentialEquations equations = new AdditiveRegulationODE(model, inferenceResult, inferenceTask, geneProfiles, profileInitialTime, profileTimeStep);
		FirstOrderIntegrator integrator = new DormandPrince54Integrator(1e-8, 1, 1e-6, 1e-3);
		List<? extends Number> targetProfile = geneProfiles.get(inferenceTask.getTargetID()).getProfile();
			
		double initialValue = targetProfile.get(0).doubleValue();
        if(initialValue < 0)
        {
        	initialValue = 0;
        }
		double endTime = targetTimePoints[targetProfile.size() - 1];
		
		ContinuousOutputModel outputModel = new ContinuousOutputModel();
		integrator.addStepHandler(outputModel);		
		integrator.integrate(equations, profileInitialTime, new double[] {initialValue}, endTime, new double[1]); //I ignore the last (output argument)
		
		double[] result = new double[targetTimePoints.length];
		for(int i = 0; i < targetProfile.size(); i++)
		{
			outputModel.setInterpolatedTime(targetTimePoints[i]);
			result[i] = outputModel.getInterpolatedState()[0];
		}
		return result;
	}
	
	public static double[] integrateNoRegulator(InferenceResult inferenceResult, NoRegulatorInferenceTask inferenceTask, List<? extends GeneProfile<?>> geneProfiles, double profileInitialTime, double[] targetTimePoints)
	{
		List<? extends Number> targetProfile = geneProfiles.get(inferenceTask.getTargetID()).getProfile();
		
        double basalSynthesis = inferenceResult.getParameters()[InferenceModel.NO_REGULATOR.getParameterIndex(InferenceModel.NO_REGULATOR_SYNTHESIS_PARAM_NAME)];
        double decay = inferenceResult.getParameters()[InferenceModel.NO_REGULATOR.getParameterIndex(InferenceModel.NO_REGULATOR_DECAY_PARAM_NAME)];

        double initialValue = targetProfile.get(0).doubleValue();
        if(initialValue < 0)
        {
        	initialValue = 0;
        }

        double derivativeAtZero = basalSynthesis - (decay * initialValue);
        double signDerivativeAtZero = (derivativeAtZero > 0) ? 1 : -1; //sign of the derivative
        double constantFactor = Math.abs(derivativeAtZero) / (-decay);
        
        double ratio = basalSynthesis / decay;
                
		double[] result = new double[targetProfile.size()];
		for(int i = 0; i < targetProfile.size(); i++)
		{
			double time = targetTimePoints[i] - profileInitialTime; //the NoRegulator inference has to start at time 0 (so I shift all times)
	        double value = signDerivativeAtZero * (constantFactor * Math.exp(-decay * time) + ratio);
			result[i] = value;
		}
		return result;
	}
}
