package cz.cas.mbu.cygenexpi.internal.tasks;

import java.lang.reflect.Constructor;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

import cz.cas.mbu.cydataseries.DataSeriesException;
import cz.cas.mbu.cydataseries.DataSeriesManager;

public class RegistrarPassingTaskFactory<TASK extends Task> extends AbstractTaskFactory {
	
	protected final CyServiceRegistrar registrar;
	private final Class<TASK> taskClass;
	private final Constructor<TASK> constructor;
	
	
	public RegistrarPassingTaskFactory(Class<TASK> taskClass, CyServiceRegistrar registrar) {
		super();
		this.taskClass = taskClass;
		this.registrar = registrar;
		
		Constructor<TASK> chosenConstructor = null;
		for(Constructor<?> candidateConstructor : taskClass.getConstructors())
		{
			Class<?> parameterTypes[] = candidateConstructor.getParameterTypes();
			if(parameterTypes.length != 1)
			{
				continue;
			}
			
			if(parameterTypes[0].equals(CyServiceRegistrar.class))
			{
				chosenConstructor = (Constructor<TASK>)candidateConstructor;
			}			
		}
		
		if(chosenConstructor == null)
		{
			throw new IllegalArgumentException("Could not find any constructor that accepts only CyServiceRegistrar.");
		}
		
		constructor = chosenConstructor;
	}


	@Override
	public TaskIterator createTaskIterator() {
		try {
			TASK task = constructor.newInstance(registrar);
			return new TaskIterator(task);		
		} catch (Exception ex)
		{
			throw new DataSeriesException("Could not create task.", ex);
		}
	}

}
