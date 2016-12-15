package cz.cas.mbu.cygenexpi.internal;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

public class CustomVizmapStyleManager implements SessionLoadedListener {

	private final CyServiceRegistrar registrar;
	
	
	
	public CustomVizmapStyleManager(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}



	@Override
	public void handleEvent(SessionLoadedEvent arg0) {
		registrar.getService(TaskManager.class).execute(new TaskIterator(new LoadCustomVizmapStyle(registrar)));		
	}

}
