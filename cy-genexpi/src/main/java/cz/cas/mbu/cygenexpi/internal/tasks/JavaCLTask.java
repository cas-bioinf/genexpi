package cz.cas.mbu.cygenexpi.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public abstract class JavaCLTask extends AbstractTask {

	protected abstract void runCLDependent(TaskMonitor taskMonitor) throws Exception;
	
	@Override
	public final void run(TaskMonitor taskMonitor) throws Exception {
		//Storing and setting the classloader is a temporary workaround for https://github.com/nativelibs4java/nativelibs4java/issues/574
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			runCLDependent(taskMonitor);
		}
		finally 
		{
			Thread.currentThread().setContextClassLoader(oldClassLoader);			
		}
	}		

}
