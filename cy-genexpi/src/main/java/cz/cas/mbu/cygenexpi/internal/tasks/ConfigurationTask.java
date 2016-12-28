package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.ListSingleSelection;

import com.nativelibs4java.opencl.CLDevice;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.cygenexpi.internal.ui.OpenCLTroubleshootingPanel;

public class ConfigurationTask extends AbstractTask {
	private final CyServiceRegistrar registrar;
	
	@Tunable(description = "Preferred device")
	public ListSingleSelection<DeviceWrapper> device;

	@Tunable(description = "Prevent full occupation (useful if your preferred device is the GPU running your display")
	public boolean preventFullOccupation;
	
	public ConfigurationTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		ConfigurationService configurationService = registrar.getService(ConfigurationService.class);
		
		List<DeviceWrapper> wrappers = configurationService.listDevices().stream()
				.map(DeviceWrapper::new)
				.collect(Collectors.toList());
		this.device = new ListSingleSelection<>(wrappers);
		preventFullOccupation = configurationService.isPreventFullOccupation();
	}


	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ConfigurationService configurationService = registrar.getService(ConfigurationService.class);

		
		
		JOptionPane.showMessageDialog(null, new OpenCLTroubleshootingPanel(), "Problem", JOptionPane.PLAIN_MESSAGE);
		
		if(device.getSelectedValue() != null)
		{
			configurationService.setPreferredDevice(device.getSelectedValue().getDevice());			
		}
		configurationService.setPreventFullOccupation(preventFullOccupation);
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
		
	}
}
