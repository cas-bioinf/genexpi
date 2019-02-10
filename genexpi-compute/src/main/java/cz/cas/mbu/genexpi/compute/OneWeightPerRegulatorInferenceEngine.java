package cz.cas.mbu.genexpi.compute;

import java.io.IOException;
import java.util.List;

import com.nativelibs4java.opencl.CLContext;

public class OneWeightPerRegulatorInferenceEngine<NUMBER_TYPE extends Number> extends BaseDerivativeInferenceEngine<NUMBER_TYPE, OneWeightPerRegulatorInferenceTask>{

	
	
	public OneWeightPerRegulatorInferenceEngine(Class<NUMBER_TYPE> elementClass, CLContext context, 
			InferenceModel model,
			EMethod method, EErrorFunction errorFunction, ELossFunction lossFunction, boolean useCustomTimeStep,
			Float customTimeStep, boolean verbose, int numIterations, boolean preventFullOccupation, int numRegulators,
			float regularizationWeight, boolean useConstitutiveExpression, boolean useFixedSeed, long fixedSeed) throws IOException {
		super(elementClass, context, model, 
				method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose,
				numIterations, preventFullOccupation, numRegulators, regularizationWeight, useFixedSeed, fixedSeed);		
	}

	@Override
	protected int[] getWeightConstraints(List<OneWeightPerRegulatorInferenceTask> inferenceTasks) {
        int numItems = inferenceTasks.size();

        int[] weightConstraints = new int[numItems * numRegulators];
        
        for(int i = 0; i < numItems; i++)
        {
        	for(int j = 0; j < numRegulators; j++)
        	{
        		weightConstraints[i * numRegulators + j] = regulationTypeToInt(inferenceTasks.get(i).getRegulationTypes()[j]);
        	}        	
        }        
		return weightConstraints;
	}

	@Override
	protected int getNumWeightConstraintsPerTask() {
		return numRegulators;
	}

	
}
