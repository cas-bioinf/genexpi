package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

public interface WizardStep<DATA> {
	void setData(DATA data, CyServiceRegistrar registrar);
	String getStepName();
	Component getComponent();
	ValidationState validate(StringBuilder messageBuilder);
	void beforeStep(TaskMonitor taskMonitor);
	void performStep(TaskMonitor taskMonitor);
	
	void wizardStarted();
	void wizardClosed();	
}
