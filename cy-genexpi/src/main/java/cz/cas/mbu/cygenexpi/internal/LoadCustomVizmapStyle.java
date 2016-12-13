package cz.cas.mbu.cygenexpi.internal;

import java.io.InputStream;
import java.util.stream.Collectors;

import org.cytoscape.io.read.VizmapReader;
import org.cytoscape.io.read.VizmapReaderManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class LoadCustomVizmapStyle extends AbstractTask {

	private final CyServiceRegistrar registrar;
	
	public LoadCustomVizmapStyle(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}



	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		VizmapReaderManager readerManager = registrar.getService(VizmapReaderManager.class);
		InputStream is = getClass().getResourceAsStream("/cz/cas/mbu/genexpi/defaultStyle.xml");
		final VizmapReader reader = readerManager.getReader(is, "GN-GPU style");
		
		insertTasksAfterCurrentTask(reader, new AbstractTask() {
			
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				if(!reader.getVisualStyles().isEmpty())
				{
					VisualStyle vs = reader.getVisualStyles().iterator().next(); //just get the first
					VisualMappingManager mappingManager = registrar.getService(VisualMappingManager.class);
										
					mappingManager.addVisualStyle(vs);
				}
				
			}
		});
		
	}

}
