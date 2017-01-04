package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaskUtils {
	public static final String NO_COLUMN_AVAILABLE_TEXT = "-- No column available --";
	
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
	
	public static boolean isNoColumnAvailable(ListSingleSelection<String> column)
	{
		return (column.getPossibleValues().size() == 1 && column.getPossibleValues().get(0).equals(NO_COLUMN_AVAILABLE_TEXT));
	}
	
	public static void updateSelectionOfTimeSeriesColumn(ListSingleSelection<CyNetwork> network, ListSingleSelection<String> column, CyServiceRegistrar registrar)
	{
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);		
		CyNetwork selectedNetwork = network.getSelectedValue();
		CyTable nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
		List<String> possibleSourceColumns = mappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());

		if(possibleSourceColumns.isEmpty())
		{
			possibleSourceColumns.add(NO_COLUMN_AVAILABLE_TEXT);
		}
		
		String previouslySelected = column.getSelectedValue();
		column.setPossibleValues(possibleSourceColumns);
		
		if(possibleSourceColumns.contains(previouslySelected))
		{
			column.setSelectedValue(previouslySelected);
		}
		
	}
	
}

