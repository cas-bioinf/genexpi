package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.cygenexpi.internal.ui.UIUtils;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

@RememberAllValues
public class MarkConstantSynthesisGenesTask extends AbstractTask {

	@Tunable(description="Column containing mapping to expression time series")
	public ListSingleSelection<String> expressionTimeSeriesColumn;
	
	@ContainsTunables
	public ErrorDef errorDef;
	
	@Tunable(description="Quality required for filtering (0-1)",groups={"Error"})
	public double requiredQuality = 0.8f;
	
	@Tunable(description="Store fits in a time series")
	public boolean storeFitsInTimeSeries = false;
	
	@Tunable(description="Name of the time series for predicted profiles", dependsOn="storeFitsInTimeSeries=true")
	public String resultsName;
	
	@Tunable(description="Store fit parameters in columns of the node table")
	public boolean storeParametersInNodeTable = false;
	
	@Tunable(description="Prefix for column names of parameters", dependsOn="storeParametersInNodeTable=true")
	public String parametersPrefix;
	
	private final CyServiceRegistrar registrar;

	private final CyTable nodeTable;
	private final CyNetwork selectedNetwork;
	
	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME); 
	
	public MarkConstantSynthesisGenesTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		selectedNetwork = applicationManager.getCurrentNetwork();
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);		
		nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
		List<String> possibleSourceColumns = mappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());
		
		expressionTimeSeriesColumn = new ListSingleSelection<>(possibleSourceColumns);
		errorDef = ErrorDef.DEFAULT;
		
		registrar.getService(RememberValueService.class).loadProperties(this);		
	}
	
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception
	{
		
		registrar.getService(RememberValueService.class).saveProperties(this);		
		PredictionService predictionService = registrar.getService(PredictionService.class);
		String resultsMappingColumnName = resultsName + "_Idx";
		try {
			predictionService.markConstantSynthesis(taskMonitor, selectedNetwork, expressionTimeSeriesColumn.getSelectedValue(), errorDef, requiredQuality, storeFitsInTimeSeries, resultsName, resultsMappingColumnName, storeParametersInNodeTable, parametersPrefix);
		} catch (SuspectGPUResetByOSException ex)
		{
			UIUtils.handleSuspectedGPUResetInTask(ex);
		}
		
	}

}
