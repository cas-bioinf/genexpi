/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.rinterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GNException;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.IntegrateResults;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;

/**
 *
 * @author MBU
 */
public class RInterface {
    public static List<CLDevice> getAllDevices() {
    	List<CLDevice> result = new ArrayList<>();
		for(CLPlatform platform : JavaCL.listPlatforms())					
		{
			for(CLDevice device: platform.listAllDevices(false))
			{
				result.add(device);
			}						
		}
    	return result;
    }
    
    public static String getDeviceDescription(CLDevice device) {
    	return device.getName() + ": " + device.getPlatform().getVersion() + ", " + device.getPlatform().getName();
    }
    
    public static List<String> getAllDevicesDescriptions() {
    	return getAllDevices().stream()
    			.map(RInterface::getDeviceDescription)
    			.collect(Collectors.toList());
    }
	
    public static InferenceResult[] computeAdditiveRegulation(DeviceSpecs deviceSpecs, List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model, int numIterations, float regularizationWeight) 
    {
    	CLContext context = deviceSpecs.createContext();
    	
        try {
        
			//GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10);
        	
			GNCompute<Float> compute = new GNCompute<Float>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, false, null); 
			List<InferenceResult> result = compute.computeAdditiveRegulation(geneProfiles, Arrays.asList(inferenceTasks), 1, numIterations, regularizationWeight, false);
			InferenceResult[] resultArray = new InferenceResult[result.size()];
			result.toArray(resultArray);
			return resultArray;
        }
        catch(GNException ex)
        {
        	throw ex;
        }
        catch(Exception ex) {
        	throw new GNException(ex);
        }
    }    
    
    public static double[][] evaluateAdditiveRegulationResult(List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model, InferenceResult[] inferenceResults, double[] targetTimePoints) {
    	double[][] result = new double[inferenceTasks.length][];
    	IntStream.range(0, inferenceTasks.length)
    			.forEach(index -> 
    				{ result[index] = IntegrateResults.integrateAdditiveRegulation(model, inferenceResults[index], inferenceTasks[index], geneProfiles, 0/*initial time*/, 1/*timestep in the profile*/, targetTimePoints); }
    			);
    	return result;
    	
    }
    
    
    public static InferenceResult[] computeConstantSynthesis(DeviceSpecs deviceSpecs, List<GeneProfile<Float>> geneProfiles, NoRegulatorInferenceTask inferenceTasks[], int numIterations)
    {
    	CLContext context = deviceSpecs.createContext();
    	
        try {
        
			GNCompute<Float> compute = new GNCompute<Float>(Float.class, context, InferenceModel.NO_REGULATOR, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, false, null);
				
			List<InferenceResult> result = compute.computeNoRegulator(geneProfiles, Arrays.asList(inferenceTasks), numIterations, false);
			InferenceResult[] resultArray = new InferenceResult[result.size()];
			result.toArray(resultArray);
			return resultArray;
        }
        catch(GNException ex)
        {
        	throw ex;
        }
        catch(Exception ex) {
        	throw new GNException(ex);
        }
    	
    }
    
    public static double[][] evaluateConstantSynthesisResult(List<GeneProfile<Float>> geneProfiles, NoRegulatorInferenceTask inferenceTasks[], InferenceResult[] inferenceResults, double[] targetTimePoints) {
    	double[][] result = new double[inferenceTasks.length][];
    	IntStream.range(0, inferenceTasks.length)
    			.forEach(index -> 
    				{ result[index] = IntegrateResults.integrateNoRegulator(inferenceResults[index], inferenceTasks[index], geneProfiles, 0/*initial time*/, targetTimePoints); }
    			);
    	return result;
    	
    }
    
}
