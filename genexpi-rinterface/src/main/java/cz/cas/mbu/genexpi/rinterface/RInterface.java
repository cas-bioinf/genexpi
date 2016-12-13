/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.rinterface;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;

/**
 *
 * @author MBU
 */
public class RInterface {
    	
    public InferenceResult[] executeComputation(List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model) 
    {
         //CLContext context = JavaCL.createBestContext();
        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU, CLPlatform.DeviceFeature.OutOfOrderQueueSupport);
        System.out.println(context);    

        try {
        
			//GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10);
        	
			GNCompute<Float> compute = new GNCompute<Float>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10, false, null); 
			List<InferenceResult> result = compute.computeAdditiveRegulation(geneProfiles, Arrays.asList(inferenceTasks), 1, 64, false);
			InferenceResult[] resultArray = new InferenceResult[result.size()];
			result.toArray(resultArray);
			return resultArray;
        }
        catch(IOException ex)
        {
        	return null;
        }
    }    
}
