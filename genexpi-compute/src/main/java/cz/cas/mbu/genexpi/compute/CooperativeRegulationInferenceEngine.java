package cz.cas.mbu.genexpi.compute;

import java.io.IOException;
import java.util.List;

import com.nativelibs4java.opencl.CLContext;

public class CooperativeRegulationInferenceEngine<NUMBER_TYPE extends Number> extends BaseSigmoidInferenceEngine<NUMBER_TYPE, CooperativeRegulationInferenceTask>{

	
	
	public CooperativeRegulationInferenceEngine(Class<NUMBER_TYPE> elementClass, CLContext context, 
			EMethod method, EErrorFunction errorFunction, ELossFunction lossFunction, boolean useCustomTimeStep,
			Float customTimeStep, boolean verbose, int numIterations, boolean preventFullOccupation, 
			float regularizationWeight, boolean useConstitutiveExpression, boolean useFixedSeed, long fixedSeed) throws IOException {
		super(elementClass, context, InferenceModel.createCooperativeRegulationModel(useConstitutiveExpression), 
				method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose,
				numIterations, preventFullOccupation, 2/*numRegulators*/, regularizationWeight, useFixedSeed, fixedSeed);		
	}

	@Override
	protected int[] getWeightConstraints(List<CooperativeRegulationInferenceTask> inferenceTasks) {
        int numItems = inferenceTasks.size();

        int[] weightConstraints = new int[numItems];
        
        for(int i = 0; i < numItems; i++)
        {
       		weightConstraints[i] = regulationTypeToInt(inferenceTasks.get(i).getRegulationType());
        }        
		return weightConstraints;
	}

	@Override
	protected int getNumWeightConstraintsPerTask() {
		return 1;
	}

	
}
