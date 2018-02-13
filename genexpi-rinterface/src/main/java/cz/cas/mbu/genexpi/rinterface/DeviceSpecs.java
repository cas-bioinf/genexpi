package cz.cas.mbu.genexpi.rinterface;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

import cz.cas.mbu.genexpi.compute.BaseInferenceEngine;
import cz.cas.mbu.genexpi.compute.ComputeUtils;

public class DeviceSpecs {
	private final CLDevice chosenDevice;
	private final CLPlatform.DeviceFeature preferredFeature;

	public DeviceSpecs() {
		chosenDevice = null;
		preferredFeature = null;
	}

	public DeviceSpecs(CLDevice device) {
		chosenDevice = device;
		preferredFeature = null;
	}

	public DeviceSpecs(CLPlatform.DeviceFeature preferredFeature) {
		this.preferredFeature = preferredFeature;
		this.chosenDevice = null;
	}
	
	public CLDevice getDevice() {
		if (chosenDevice != null) {
			return chosenDevice;
		} else if (preferredFeature != null) {
			return JavaCL.getBestDevice(preferredFeature, DeviceFeature.MaxComputeUnits,
					DeviceFeature.OutOfOrderQueueSupport);
		} else {
			return ComputeUtils.getBestDevice();
		}
		
	}

	public CLContext createContext() {
		CLDevice device = getDevice();
		return device.getPlatform().createContext(null, device);
	}
	
	public String getDeviceDescription2() {
		CLDevice device = getDevice();
		return device.getPlatform().getName() + ": " + device.getName();
	}
	
	public static DeviceSpecs gpuSpecs() {
		return new DeviceSpecs(CLPlatform.DeviceFeature.GPU);
	}
	
	public static DeviceSpecs processorSpecs() {
		return new DeviceSpecs(CLPlatform.DeviceFeature.CPU);
	}
	
}
