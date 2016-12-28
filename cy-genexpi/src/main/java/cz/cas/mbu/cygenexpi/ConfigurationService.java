package cz.cas.mbu.cygenexpi;

import java.util.List;

import com.nativelibs4java.opencl.CLDevice;

public interface ConfigurationService {
	boolean wasConfigured();
	
	void testDevice(CLDevice device);
	
	List<CLDevice> listDevices();
	/**
	 * Possibly null, if no preferred device configured.
	 * @return
	 */
	CLDevice getPreferredDevice();
	void setPreferredDevice(CLDevice device);
	
	boolean isPreventFullOccupation();
	void setPreventFullOccupation(boolean preventFullOccupation);
	
}
