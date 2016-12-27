package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaskUtils {
	public static void updateSelectionOfTimeSeriesColumn(ListSingleSelection<CyNetwork> network, ListSingleSelection<String> column, CyServiceRegistrar registrar)
	{
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);		
		CyNetwork selectedNetwork = network.getSelectedValue();
		CyTable nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
		List<String> possibleSourceColumns = mappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());

		String previouslySelected = column.getSelectedValue();
		column.setPossibleValues(possibleSourceColumns);
		
		if(possibleSourceColumns.contains(previouslySelected))
		{
			column.setSelectedValue(previouslySelected);
		}
		
	}
	
}
