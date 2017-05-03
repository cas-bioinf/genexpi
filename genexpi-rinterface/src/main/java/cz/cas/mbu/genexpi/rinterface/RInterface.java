/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.rinterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GNException;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;

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
	
    public static InferenceResult[] computeAdditiveRegulation(DeviceSpecs deviceSpecs, List<GeneProfile<Float>> geneProfiles, AdditiveRegulationInferenceTask inferenceTasks[], InferenceModel model) 
    {
    	CLContext context = deviceSpecs.createContext();
    	
        System.out.println(context);    

        try {
        
			//GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10);
        	
			GNCompute<Float> compute = new GNCompute<Float>(Float.class, context, model, EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10, false, null); 
			List<InferenceResult> result = compute.computeAdditiveRegulation(geneProfiles, Arrays.asList(inferenceTasks), 1, 64, false);
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
}
