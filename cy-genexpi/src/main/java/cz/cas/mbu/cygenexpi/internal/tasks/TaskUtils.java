package cz.cas.mbu.cygenexpi.internal.tasks;

<<<<<<< HEAD
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
=======
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

>>>>>>> master
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
<<<<<<< HEAD
=======
import org.cytoscape.work.util.ListSingleSelection;
>>>>>>> master

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaskUtils {
<<<<<<< HEAD
	public static boolean expressionDataAvailable(CyServiceRegistrar registrar)
	{
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		CyNetwork selectedNetwork = applicationManager.getCurrentNetwork();
		
		if(selectedNetwork == null)
		{
			return false;
		}
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
		
=======
	public static void updateSelectionOfTimeSeriesColumn(ListSingleSelection<CyNetwork> network, ListSingleSelection<String> column, CyServiceRegistrar registrar)
	{
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);		
		CyNetwork selectedNetwork = network.getSelectedValue();
>>>>>>> master
		CyTable nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
<<<<<<< HEAD
		boolean anyAvailableMappings = mappings.keySet().stream()
				.anyMatch(col -> (nodeTable.getColumn(col) != null));
		
		return anyAvailableMappings;
		
	}
=======
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
	
>>>>>>> master
}
