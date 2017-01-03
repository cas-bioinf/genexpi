package cz.cas.mbu.cygenexpi.internal.tasks;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.cygenexpi.internal.ConfigurationHelp;

public class CheckConfigurationTask extends AbstractTask {

	private final CyServiceRegistrar registrar;
		
	
	public CheckConfigurationTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}



	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if(!registrar.getService(ConfigurationService.class).wasConfigured())
		{
			String configurationMessage = "Genexpi was not configured or the configuration is invalid. Please configure Genexpi before proceeding.";
			if(GraphicsEnvironment.isHeadless())
			{
				taskMonitor.showMessage(Level.WARN, configurationMessage);
			}
			
			ConfigurationTask configurationTask = new ConfigurationTask(registrar);			
			configurationTask.help = new ConfigurationHelp(configurationTask.help.getMainMessage(), configurationMessage + "\n\n" + configurationTask.help.getDetails());
			insertTasksAfterCurrentTask(configurationTask);
			
		}
	}
	
}
