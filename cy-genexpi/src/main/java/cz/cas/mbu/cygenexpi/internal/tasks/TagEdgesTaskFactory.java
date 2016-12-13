package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TagEdgesTaskFactory extends AbstractTaskFactory {

	private final CyServiceRegistrar registrar;
	
	public TagEdgesTaskFactory(CyServiceRegistrar registrar) {
		this.registrar = registrar;
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
		CyTable edgeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> nodeMappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		Map<String, TimeSeries> edgeMappings = mappingManager.getAllMappings(selectedNetwork, CyEdge.class, TimeSeries.class);
		
		boolean anyAvailableNodeMappings = nodeMappings.keySet().stream()
				.anyMatch(col -> (nodeTable.getColumn(col) != null));
		boolean anyAvailableEdgeMappings = edgeMappings.keySet().stream()
				.anyMatch(col -> (edgeTable.getColumn(col) != null));
		
		return anyAvailableNodeMappings && anyAvailableEdgeMappings;
	}


	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new TagEdgesTask(registrar));
	}

	
}
