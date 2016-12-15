package cz.cas.mbu.cygenexpi.internal;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.tasks.ExpressionDependentRegistrarTaskFactory;
import cz.cas.mbu.cygenexpi.internal.tasks.MarkConstantSynthesisGenesTask;
import cz.cas.mbu.cygenexpi.internal.tasks.MarkNoChangeGenesTask;
import cz.cas.mbu.cygenexpi.internal.tasks.NetworkSelectedRegistrarTaskFactory;
import cz.cas.mbu.cygenexpi.internal.tasks.PredictSingleRegulationsTask;
import cz.cas.mbu.cygenexpi.internal.tasks.StartWizardTask;
import cz.cas.mbu.cygenexpi.internal.tasks.TagEdgesTaskFactory;
import cz.cas.mbu.cygenexpi.internal.tasks.TagNodesTask;
import cz.cas.mbu.cygenexpi.internal.ui.wizard.InferenceWizard;

public class CyActivator extends AbstractCyActivator {
	
	protected void registerTaskFactory(BundleContext bc, TaskFactory taskFactory, String title)
	{
		Properties properties = new Properties();
		properties.setProperty(ServiceProperties.PREFERRED_MENU,"Apps.Genexpi");
		properties.setProperty(ServiceProperties.IN_MENU_BAR,"true");		
		properties.setProperty(ServiceProperties.TITLE, title);
		
		registerService(bc, taskFactory, TaskFactory.class, properties);
		
	}
	
	@Override
	public void start(BundleContext bc) throws Exception {
		
		CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);		
		
		registerService(bc, new RememberValueServiceImpl(), RememberValueService.class, new Properties());
		registerService(bc, new TaggingServiceImpl(), TaggingService.class, new Properties());
		registerService(bc, new PredictionServiceImpl(serviceRegistrar), PredictionService.class, new Properties());	
		

		registerTaskFactory(bc, new NetworkSelectedRegistrarTaskFactory<>(StartWizardTask.class, serviceRegistrar),
				InferenceWizard.TITLE);
		registerTaskFactory(bc, new ExpressionDependentRegistrarTaskFactory<>(MarkNoChangeGenesTask.class, serviceRegistrar), 
				"Mark genes that are approximately constant onver the time range.");		
		registerTaskFactory(bc, new ExpressionDependentRegistrarTaskFactory<>(MarkConstantSynthesisGenesTask.class, serviceRegistrar),
				 "Mark genes that have constant synthesis");		
		registerTaskFactory(bc, new ExpressionDependentRegistrarTaskFactory<>(PredictSingleRegulationsTask.class, serviceRegistrar),
				"Predict single factor regulations");
		registerTaskFactory(bc, new ExpressionDependentRegistrarTaskFactory<>(TagNodesTask.class, serviceRegistrar),
				"Tag node profiles");
		registerTaskFactory(bc, new TagEdgesTaskFactory(serviceRegistrar),
				"Tag edge profiles");
		
		registerService(bc, new CustomVizmapStyleManager(serviceRegistrar), SessionLoadedListener.class, new Properties());
	}

}
