package cz.cas.mbu.cygenexpi.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.cygenexpi.GNException;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;

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
	public void testDevice(CLDevice device) throws GNException {
		JavaCLHelper.runWithCLClassloader(() -> {
			//Create a simple simple data
			try {
				CLContext context = device.getPlatform().createContext(Collections.EMPTY_MAP, device);
				GNCompute<Float> compute = new GNCompute<Float>(Float.class, context, InferenceModel.createAdditiveRegulationModel(1), EMethod.Annealing, EErrorFunction.Euler, ELossFunction.Squared, 10.0f, false, 0.0f);
				GeneProfile<Float> p1 = new GeneProfile<>("test1", Arrays.asList(0.f,0.1f,0.2f,0.3f,0.4f));
				GeneProfile<Float> p2 = new GeneProfile<>("test2", Arrays.asList(0.4f,0.3f,0.2f,0.1f,0.0f));
				
				compute.computeAdditiveRegulation(Arrays.asList(p1, p2), Collections.singletonList(new AdditiveRegulationInferenceTask(0, 1)), 1, 64, true);
			}
			catch(GNException ex)
			{
				throw ex;
			}
			catch(Exception ex)
			{
				throw new GNException(ex.getMessage(), ex);
			}
		});
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
