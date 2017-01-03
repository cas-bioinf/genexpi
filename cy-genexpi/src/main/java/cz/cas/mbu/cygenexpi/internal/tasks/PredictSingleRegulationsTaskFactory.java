package cz.cas.mbu.cygenexpi.internal.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class PredictSingleRegulationsTaskFactory extends AbstractTaskFactory{
	private final CyServiceRegistrar registrar;

	public PredictSingleRegulationsTaskFactory(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new CheckConfigurationTask(registrar), new PredictSingleRegulationsTask(registrar));
	}
	
	@Override
	public boolean isReady() {
		if(!super.isReady())
		{
			return false;
		}

		return TaskUtils.expressionDataAvailable(registrar);
	}
	
	
}
