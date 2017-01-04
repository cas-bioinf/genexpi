package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import com.nativelibs4java.opencl.CLDevice;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.cygenexpi.internal.ConfigurationHelp;
import cz.cas.mbu.cygenexpi.internal.ConfigurationHelp.MainMessage;

public class ConfigurationTask extends AbstractValidatedTask {
	private final CyServiceRegistrar registrar;
	private final Logger logger = Logger.getLogger(ConfigurationTask.class);

	@Tunable(groups = "Information")
	public ConfigurationHelp help;
	
	@Tunable(description = "Device for computation")
	public ListSingleSelection<DeviceWrapper> device;

	@Tunable(description = "Prevent full device occupation")
	public boolean preventFullOccupation;
	
	public ConfigurationTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		ConfigurationService configurationService = registrar.getService(ConfigurationService.class);
		
		try {
			List<CLDevice> allDevices = configurationService.listDevices();
			List<DeviceWrapper> wrappers = allDevices.stream()
					.map(DeviceWrapper::new)
					.collect(Collectors.toList());
			this.device = new ListSingleSelection<>(wrappers);
		
			CLDevice preferredDevice = configurationService.getPreferredDevice();
			if(preferredDevice != null)
			{
				Optional<DeviceWrapper> selected = wrappers.stream()
						.filter(w -> w.getDevice().equals(preferredDevice))
						.findAny();
				if(selected.isPresent())
				{
					device.setSelectedValue(selected.get());
				}
			}
			
			if(allDevices.isEmpty())
			{
				help = new ConfigurationHelp(ConfigurationHelp.MainMessage.NoDevices, "");
			}
			else
			{
				help = new ConfigurationHelp(ConfigurationHelp.MainMessage.None, "");			
			}
		}
		catch(Exception ex)
		{
			logger.error(ex);
			help = new ConfigurationHelp(MainMessage.InitError, ex.getMessage() == null ? "" : ex.getMessage());
		}
		
		preventFullOccupation = configurationService.isPreventFullOccupation();
	}


	@ProvidesTitle
	public String getTitle()
	{
		return "CyGenexpi configuration";
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ConfigurationService configurationService = registrar.getService(ConfigurationService.class);
					
		
		boolean setActualValues = true;
		CLDevice clDevice = device.getSelectedValue().getDevice();
		if(device.getSelectedValue() != null)
		{
			taskMonitor.setStatusMessage("Testing the selected device.");
			try 
			{
				configurationService.testDevice(clDevice);				
			}
			catch(Throwable ex)
			{
				logger.error(ex);
				
				setActualValues = false;
				ConfigurationTask nextTask = new ConfigurationTask(registrar);
				nextTask.device.setSelectedValue(device.getSelectedValue());
				nextTask.preventFullOccupation = preventFullOccupation;
				
				nextTask.help = new ConfigurationHelp(ConfigurationHelp.MainMessage.DeviceTestFailed, "Using device: " + device.getSelectedValue().toString() + "\n" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
				
				insertTasksAfterCurrentTask(nextTask);
			}
		}
		
		if(setActualValues)
		{
			if(device.getSelectedValue() != null)
			{
				configurationService.setPreferredDevice(clDevice);			
			}		
			configurationService.setPreventFullOccupation(preventFullOccupation);
		}
	}



	
	
	@Override
	protected ValidationState getValidationState(StringBuilder messageBuilder) {
		DeviceWrapper selectedDeviceWrapper = device.getSelectedValue();
		if(selectedDeviceWrapper != null && selectedDeviceWrapper.getDevice().getType().contains(CLDevice.Type.GPU) && !preventFullOccupation)
		{
			messageBuilder.append("You have selected a GPU platform.\n"
					+ "If this is the GPU that runs your main display, computations may not be succesful and may freeze your computer.\n"
					+ "If it is your main display, you should select 'Prevent full occupation'"
					+ "\nAre you sure '" + selectedDeviceWrapper.getDevice().getName() + "' is not your main display?\n"
					);
			return ValidationState.REQUEST_CONFIRMATION;
		}
		return ValidationState.OK;
	}





	private static class DeviceWrapper {
		private final CLDevice device;

		public DeviceWrapper(CLDevice device) {
			super();
			this.device = device;
		}

		public CLDevice getDevice() {
			return device;
		}

		@Override
		public String toString() {
			return device.getName() + ": " + device.getPlatform().getVersion() + ", " + device.getPlatform().getName();
		}

		@Override
		public int hashCode() {
			return device.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DeviceWrapper other = (DeviceWrapper) obj;
			if (device == null) {
				if (other.device != null)
					return false;
			} else if (!device.equals(other.device))
				return false;
			return true;
		}		
		
		
	}
}
