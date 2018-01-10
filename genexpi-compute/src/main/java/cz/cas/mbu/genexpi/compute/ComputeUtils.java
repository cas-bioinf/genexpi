package cz.cas.mbu.genexpi.compute;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

public class ComputeUtils {
    public static CLDevice getBestDevice()
    {
    	CLDevice device = JavaCL.getBestDevice(CLPlatform.DeviceFeature.OutOfOrderQueueSupport, CLPlatform.DeviceFeature.GPU, CLPlatform.DeviceFeature.MaxComputeUnits);
    	if(device != null)
    	{
    		return device;
    	}
    	device = JavaCL.getBestDevice(CLPlatform.DeviceFeature.OutOfOrderQueueSupport, CLPlatform.DeviceFeature.MaxComputeUnits);
    	if(device != null)
    	{
    		return device;
    	}
    	return JavaCL.getBestDevice();    	
    	
    }
    
    /**
     * 
     * @return null if no context available
     */
    public static CLContext getBestContext()
    {
    	CLDevice device = getBestDevice();
    	return device.getPlatform().createContext(null, device);    	
    }

}
