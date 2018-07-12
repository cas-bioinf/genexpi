package cz.cas.mbu.genexpi.compute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.LocalSize;

public abstract class BaseSigmoidInferenceEngine<NUMBER_TYPE extends Number, TASK_TYPE extends BaseSigmoidInferenceTask> extends BaseInferenceEngine<NUMBER_TYPE, TASK_TYPE>{
	
	protected final int numRegulators;
	protected final float regularizationWeight; 
	
	
	
	public BaseSigmoidInferenceEngine(Class<NUMBER_TYPE> elementClass, CLContext context, InferenceModel model,
			EMethod method, EErrorFunction errorFunction, ELossFunction lossFunction, boolean useCustomTimeStep,
			Float customTimeStep, boolean verbose, int numIterations, boolean preventFullOccupation, int numRegulators,
			float regularizationWeight, boolean useFixedSeed, long fixedSeed) throws IOException {
		super(elementClass, context, model, method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep,
				verbose, numIterations, preventFullOccupation, useFixedSeed, fixedSeed);
		this.numRegulators = numRegulators;
		this.regularizationWeight = regularizationWeight;
	}

	protected abstract int[] getWeightConstraints(List<TASK_TYPE> inferenceTasks);
	protected abstract int getNumWeightConstraintsPerTask();
	
    @Override
	public List<InferenceResult> compute(List<GeneProfile<NUMBER_TYPE>> geneProfiles, List<TASK_TYPE> inferenceTasks) {
    	if(inferenceTasks.isEmpty())
    	{
    		return Collections.EMPTY_LIST;
    	}
    	
        long preparationStartTime = System.nanoTime();
        CLQueue queue = createQueue();
        long totalBytes = 0;
        
        int numItems = inferenceTasks.size();

        int[] regulatorIDs = new int[numItems * numRegulators];
        int[] targetIDs = new int[numItems];
        int[] weightConstraints = getWeightConstraints(inferenceTasks);
        
        for(int i = 0; i < numItems; i++)
        {
        	targetIDs[i] = inferenceTasks.get(i).getTargetID();
        	
        	int[] taskRegulatorIDs = inferenceTasks.get(i).getRegulatorIDs();
        	if(numRegulators != taskRegulatorIDs.length){
        		throw new GNException("Inconsistent regulator numbers");
        	}
        	for(int j = 0; j < numRegulators; j++)
        	{
        		regulatorIDs[i * numRegulators + j] = taskRegulatorIDs[j];
        	}        	
        }
        
        List<Object> argumentList = new ArrayList<>();
        
        int numTimePoints = geneProfiles.get(0).getProfile().size();
        
        totalBytes += prepareXorShiftParameters(argumentList, numItems);
        totalBytes += prepareBaseParameters(argumentList, geneProfiles, targetIDs);
        argumentList.add(numItems);
        argumentList.add(numIterations);
        totalBytes += prepareProfileIDParameter(argumentList, regulatorIDs);
        totalBytes += prepareWeightConstraintsParameter(argumentList, weightConstraints);
        argumentList.add(getLocalSizeByElementClass(numTimePoints * numRegulators)); //local profiles
        argumentList.add(getLocalSizeByElementClass(numRegulators + 1)); //local profile maxima (including target)
        argumentList.add(regularizationWeight);
        argumentList.add(LocalSize.ofIntArray(getNumWeightConstraintsPerTask())); //local weight constraints
        
        OutputPointers outputPointers = prepareOutputParameters(argumentList, numItems);
        totalBytes += outputPointers.getByteCount();
                      
        long totalMB = totalBytes / (1024 * 1024);
        
        if(verbose) {
        	System.out.println("Allocating " + totalMB + "MB.");
        }

        kernel.setArgs(argumentList.toArray());
        
        long mainStartTime = System.nanoTime();
        long preparationDuration = mainStartTime - preparationStartTime;
        
        float preparationDurationMSec = (((float)preparationDuration) / 1000000);
        if(verbose) {
        	System.out.println("Preparation took: " + preparationDurationMSec + " ms.");        
        }
        
        CLEvent[] eventsToWaitForArray = executeKernel(queue, numItems);

        List<InferenceResult> results = gatherInferenceResults(queue, outputPointers, eventsToWaitForArray, numItems);
        queue.finish();
        long mainDuration = System.nanoTime() - mainStartTime;
   
        float mainDurationMSec = (((float)mainDuration) / 1000000);

        if(verbose) {
        	System.out.println("Computation took: " + mainDurationMSec + " ms.");        
        }
        return results;
    }

}
