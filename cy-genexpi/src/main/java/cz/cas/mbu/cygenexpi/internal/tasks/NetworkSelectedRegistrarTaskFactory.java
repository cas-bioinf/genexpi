package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.Arrays;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Task;

public class NetworkSelectedRegistrarTaskFactory<TASK extends Task> extends RegistrarPassingTaskFactory<TASK>{
	
	public NetworkSelectedRegistrarTaskFactory(Class<TASK> taskClass, CyServiceRegistrar registrar) {
		super(taskClass, registrar);
	}


	@Override
	public boolean isReady() {
		return registrar.getService(CyApplicationManager.class).getCurrentNetwork() != null;
	}

}
