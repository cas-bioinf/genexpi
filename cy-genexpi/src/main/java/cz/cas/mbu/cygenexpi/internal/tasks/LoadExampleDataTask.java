package cz.cas.mbu.cygenexpi.internal.tasks;

import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.io.read.CySessionReader;
import org.cytoscape.io.read.CySessionReaderManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionAboutToBeLoadedEvent;
import org.cytoscape.session.events.SessionLoadCancelledEvent;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

public class LoadExampleDataTask extends AbstractTask {
	@Tunable(description="<html>Current session (all networks and tables) will be lost.<br />Do you want to continue?</html>",
			 params="ForceSetDirectly=true;ForceSetTitle=Open Session")
	public boolean approve;

	private final CyServiceRegistrar registrar;		
	
	public LoadExampleDataTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if(approve)
		{
			CyEventHelper eventHelper = registrar.getService(CyEventHelper.class);
			eventHelper.fireEvent(new SessionAboutToBeLoadedEvent(this));
			try {
				InputStream is = getClass().getResourceAsStream("/cz/cas/mbu/genexpi/example1_ds.cys");
				File tempFile = File.createTempFile("cygenexpi_example", ".cys");
				tempFile.deleteOnExit();
				Files.copy(is, tempFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
				
				CySessionReader reader = registrar.getService(CySessionReaderManager.class).getReader(tempFile.toURI(), tempFile.getName());
				
				if (reader == null)
				{
					throw new NullPointerException("Failed to find appropriate reader for example data.");
				}
				reader.run(taskMonitor);
				
				final CySession newSession = reader.getSession();
				
				if (newSession == null)
					throw new NullPointerException("Example session could not be read.");

				CySessionManager sessionMgr = registrar.getService(CySessionManager.class);
				sessionMgr.setCurrentSession(newSession, "CyGenexpi example");
				
			}
			catch(Exception ex)
			{
				eventHelper.fireEvent(new SessionLoadCancelledEvent(this));
				throw ex;
			}
		}
		
	}
	
	
}
