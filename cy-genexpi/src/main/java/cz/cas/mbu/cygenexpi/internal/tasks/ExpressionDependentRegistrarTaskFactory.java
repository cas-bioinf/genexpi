package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Task;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class ExpressionDependentRegistrarTaskFactory<TASK extends Task> extends RegistrarPassingTaskFactory<TASK>{
	
	public ExpressionDependentRegistrarTaskFactory(Class<TASK> taskClass, CyServiceRegistrar registrar) {
		super(taskClass, registrar);
	}


	@Override
	public boolean isReady() {
		if(!super.isReady())
		{
			return false;
		}
		
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		CyNetwork selectedNetwork = applicationManager.getCurrentNetwork();
		
		if(selectedNetwork == null)
		{
			return false;
		}
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
		
		CyTable nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
		boolean anyAvailableMappings = mappings.keySet().stream()
				.anyMatch(col -> (nodeTable.getColumn(col) != null));
		
		return anyAvailableMappings;
	}

}
