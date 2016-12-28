package cz.cas.mbu.cygenexpi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;

import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.cygenexpi.ConfigurationService;

public class ConfigurationServiceImpl implements ConfigurationService {
	private final CyServiceRegistrar registrar;

	private static final String PREFERRED_DEVICE = "cz.cas.mbu.genexpi.preferredDevice";
	private static final String PREFERRED_PLATFORM = "cz.cas.mbu.genexpi.preferredPlatform";
	private static final String PREFERRED_VERSION = "cz.cas.mbu.genexpi.preferredVersion";
	private static final String PREVENT_FULL_OCCUPATION = "cz.cas.mbu.genexpi.preventFullOccupation";
	
	
	public ConfigurationServiceImpl(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}

	private Properties getProperties()
	{
		CyProperty<Properties> cyPropertyServiceRef = registrar.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		return cyPropertyServiceRef.getProperties();
	}
	
	private Stream<CLDevice> deviceStream()
	{
		//Not exactly efficient, but who cares
		List<CLDevice> value = new ArrayList<>();
		for(CLPlatform platform : JavaCL.listPlatforms())
		{
			for(CLDevice device : platform.listAllDevices(false))
			{
				value.add(device);
			}
		}
		return value.stream();
	}
	
	@Override
	public boolean wasConfigured() {
		return getPreferredDevice() != null;
	}

	@Override
	public void testDevice(CLDevice device) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CLDevice> listDevices() {
		List<CLDevice> value = new ArrayList<>();
		deviceStream().forEach(value::add);
		return value;
	}

	@Override
	public CLDevice getPreferredDevice() {
		Properties props = getProperties();
		String platformName = props.getProperty(PREFERRED_PLATFORM, "");
		String platformVersion = props.getProperty(PREFERRED_VERSION, "");
		String deviceName = props.getProperty(PREFERRED_DEVICE, "");
		
		if(platformName.isEmpty() || platformVersion.isEmpty() || deviceName.isEmpty())
		{
			return null;
		}
		
		Optional<CLDevice> preferredDevice = deviceStream().filter(
				candidate -> 
					candidate.getPlatform().getName().equals(platformName) 
					&& candidate.getPlatform().getVersion().equals(platformVersion)
					&& candidate.getName().equals(deviceName)
				).findAny();
		
		if(preferredDevice.isPresent())
		{
			return preferredDevice.get();
		}
		else
		{
			return null;
		}
	}

	@Override
	public void setPreferredDevice(CLDevice device) {
		Properties props = getProperties();
		props.setProperty(PREFERRED_PLATFORM, device.getPlatform().getName());
		props.setProperty(PREFERRED_VERSION, device.getPlatform().getVersion());
		props.setProperty(PREFERRED_DEVICE, device.getName());
	}

	@Override
	public boolean isPreventFullOccupation() {
		return getProperties().getProperty(PREVENT_FULL_OCCUPATION, Boolean.toString(false)).equals(Boolean.toString(true));
	}

	@Override
	public void setPreventFullOccupation(boolean preventFullOccupation) {
		getProperties().setProperty(PREVENT_FULL_OCCUPATION, Boolean.toString(preventFullOccupation));
		
	}
	
	
}
