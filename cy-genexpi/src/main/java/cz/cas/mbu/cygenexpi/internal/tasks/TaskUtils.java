package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaskUtils {
	public static boolean expressionDataAvailable(CyServiceRegistrar registrar)
	{
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
