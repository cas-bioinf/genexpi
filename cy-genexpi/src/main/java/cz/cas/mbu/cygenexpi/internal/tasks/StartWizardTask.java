package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.Properties;

import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.internal.ui.UIUtils;
import cz.cas.mbu.cygenexpi.internal.ui.wizard.InferenceWizard;
import cz.cas.mbu.cygenexpi.internal.ui.wizard.GNWizardData;
import cz.cas.mbu.cygenexpi.internal.ui.wizard.WizardPanel;
import cz.cas.mbu.cygenexpi.internal.ui.wizard.WizardStep;

public class StartWizardTask extends AbstractTask{

	private final CyServiceRegistrar registrar;
		
	
	public StartWizardTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}


	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		GNWizardData data = new GNWizardData();

		data.selectedNetwork = registrar.getService(CyApplicationManager.class).getCurrentNetwork();
		
		WizardPanel<GNWizardData> panel = new WizardPanel<>(registrar, InferenceWizard.getSteps(), InferenceWizard.TITLE, data);
		registrar.registerService(panel, CytoPanelComponent.class, new Properties());
		
		UIUtils.ensurePanelVisible(registrar, panel);		


		
	}

	
}
