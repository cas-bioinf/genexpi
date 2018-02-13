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
import cz.cas.mbu.genexpi.compute.BaseInferenceEngine;
import cz.cas.mbu.genexpi.compute.GNException;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.IInferenceEngine;
import cz.cas.mbu.genexpi.compute.InferenceEngineBuilder;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.IntegrateResults;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;

/**
 *
 * @author MBU
 */
public class RInterface {
	private static boolean verbose = true;
	
    public static List<CLDevice> getAllDevices() {
    	try {
	    	List<CLDevice> result = new ArrayList<>();
			for(CLPlatform platform : JavaCL.listPlatforms())					
			{
				for(CLDevice device: platform.listAllDevices(false))
				{
					result.add(device);
				}						
			}
	    	return result;
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}
    }
    
    public static void setVerbose(boolean verbose) {
    	RInterface.verbose = verbose;
    }
    
    public static String getDeviceDescription(CLDevice device) {
    	try {
    		return device.getName() + ": " + device.getPlatform().getVersion() + ", " + device.getPlatform().getName();
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}
    }
    
    public static List<String> getAllDevicesDescriptions() {
    	try {
    		return getAllDevices().stream()
    			.map(RInterface::getDeviceDescription)
    			.collect(Collectors.toList());
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}
    }
	
    public static List<GeneProfile<Float>> geneProfilesFromMatrix(double[][] profileMatrix, String[] profileNames) {
    	try {
	    	List<GeneProfile<Float>> result = new ArrayList<>(profileMatrix.length);
	    	for(int i = 0; i < profileMatrix.length; i++)
	    	{
	    		List<Float> profileValues = Arrays.stream(profileMatrix[i])
	    				.mapToObj(x -> (float)x)
	    				.collect(Collectors.toList());
	    		GeneProfile<Float> profile = new GeneProfile<>(profileNames[i], profileValues);
	    		result.add(profile);
	    	}
	    	return result;
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}	    	
    }
    
    public static InferenceResult[] computeAdditiveRegulation(DeviceSpecs deviceSpecs, List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model, int numRegulators, int numIterations, float regularizationWeight) 
    {
        try {
        	CLContext context = deviceSpecs.createContext();
        
			//GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10);
        	 
			IInferenceEngine<Float, AdditiveRegulationInferenceTask> compute = new InferenceEngineBuilder<>(Float.class)
					.setContext(context)
					.setErrorFunction(EErrorFunction.Euler)
					.setLossFunction(ELossFunction.Squared)
					.setVerbose(verbose)
					.setNumIterations(numIterations)
					.buildAdditiveRegulation(numRegulators, false, regularizationWeight);
					
			List<InferenceResult> result = compute.compute(geneProfiles, Arrays.asList(inferenceTasks));
			InferenceResult[] resultArray = new InferenceResult[result.size()];
			result.toArray(resultArray);
			return resultArray;
        }
        catch(GNException ex)
        {
        	ex.printStackTrace();
        	throw ex;
        }
        catch(Exception ex) {
        	ex.printStackTrace();
        	throw new GNException(ex);
        }
    }    
    
    public static double[][] evaluateAdditiveRegulationResult(List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model, InferenceResult[] inferenceResults, double[] targetTimePoints) {
    	try {
	    	double[][] result = new double[inferenceTasks.length][];
	    	IntStream.range(0, inferenceTasks.length)
	    			.forEach(index -> 
	    				{ result[index] = IntegrateResults.integrateAdditiveRegulation(model, inferenceResults[index], inferenceTasks[index], geneProfiles, 0/*initial time*/, 1/*timestep in the profile*/, targetTimePoints); }
	    			);
	    	return result;
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}	    	
    	
    }
    
    
    public static InferenceResult[] computeConstantSynthesis(DeviceSpecs deviceSpecs, List<GeneProfile<Float>> geneProfiles, NoRegulatorInferenceTask inferenceTasks[], int numIterations)
    {   	
        try {
        	CLContext context = deviceSpecs.createContext();
        
			IInferenceEngine<Float, NoRegulatorInferenceTask> compute = new InferenceEngineBuilder<>(Float.class)
					.setContext(context)
					.setErrorFunction(EErrorFunction.Euler)
					.setLossFunction(ELossFunction.Squared)
					.setVerbose(verbose)
					.setNumIterations(numIterations)
					.buildNoRegulator();
									
			List<InferenceResult> result = compute.compute(geneProfiles, Arrays.asList(inferenceTasks));
			InferenceResult[] resultArray = new InferenceResult[result.size()];
			result.toArray(resultArray);
			return resultArray;
        }
        catch(GNException ex)
        {
        	ex.printStackTrace();
        	throw ex;
        }
        catch(Exception ex) {
        	ex.printStackTrace();
        	throw new GNException(ex);
        }
    	
    }
    
    public static double[][] evaluateConstantSynthesisResult(List<GeneProfile<Float>> geneProfiles, NoRegulatorInferenceTask inferenceTasks[], InferenceResult[] inferenceResults, double[] targetTimePoints) {
    	try {
	    	double[][] result = new double[inferenceTasks.length][];
	    	IntStream.range(0, inferenceTasks.length)
	    			.forEach(index -> 
	    				{ result[index] = IntegrateResults.integrateNoRegulator(inferenceResults[index], inferenceTasks[index], geneProfiles, 0/*initial time*/, targetTimePoints); }
	    			);
	    	return result;
    	} catch(RuntimeException ex) {
    		ex.printStackTrace();
    		throw ex;
    	}	    	
    	
    }
    
}
