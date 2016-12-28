package cz.cas.mbu.cygenexpi.internal.tasks;

import javax.swing.JOptionPane;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.cygenexpi.internal.ui.OpenCLTroubleshootingPanel;

public class CheckAtLeastOneDeviceTask extends AbstractTask {
	
	private final CyServiceRegistrar registrar;
	
	public CheckAtLeastOneDeviceTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ConfigurationService configurationService = registrar.getService(ConfigurationService.class);
		
		if(configurationService.listDevices().isEmpty())
		{
			OpenCLTroubleshootingPanel panel = new OpenCLTroubleshootingPanel();
			panel.setSpecificMessage("No devices found");
			JOptionPane.showMessageDialog(null, panel);
		}
		
	}

}
