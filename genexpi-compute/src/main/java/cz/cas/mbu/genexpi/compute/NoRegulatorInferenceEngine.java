package cz.cas.mbu.genexpi.compute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class NoRegulatorInferenceEngine<NUMBER_TYPE extends Number> extends BaseInferenceEngine<NUMBER_TYPE, NoRegulatorInferenceTask>{
	
	
	
	public NoRegulatorInferenceEngine(Class<NUMBER_TYPE> elementClass, CLContext context, 
			EMethod method, EErrorFunction errorFunction, ELossFunction lossFunction, boolean useCustomTimeStep,
			Float customTimeStep, boolean verbose, int numIterations, boolean preventFullOccupation, boolean useFixedSeed, long fixedSeed)
			throws IOException {
		super(elementClass, context, InferenceModel.NO_REGULATOR, method, errorFunction, lossFunction, useCustomTimeStep, customTimeStep, verbose,
				numIterations, preventFullOccupation, useFixedSeed, fixedSeed);
	}

	@Override
    public List<InferenceResult> compute(List<GeneProfile<NUMBER_TYPE>> geneProfiles, List<NoRegulatorInferenceTask> inferenceTasks)
    {
    	if(inferenceTasks.isEmpty())
    	{
    		return Collections.EMPTY_LIST;
    	}
    	
        long preparationStartTime = System.nanoTime();
        
        CLQueue queue = createQueue();
        
        long totalBytes = 0;
        
        int numItems = inferenceTasks.size();

        int[] targetIDs = new int[numItems];
        
        for(int i = 0; i < numItems; i++)
        {
        	targetIDs[i] = inferenceTasks.get(i).getTargetID();        	
        }
        
        List<Object> argumentList = new ArrayList<>();
        
        totalBytes += prepareXorShiftParameters(argumentList, numItems);
        totalBytes += prepareBaseParameters(argumentList, geneProfiles, targetIDs);
        argumentList.add(numItems);
        argumentList.add(numIterations);
        
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
