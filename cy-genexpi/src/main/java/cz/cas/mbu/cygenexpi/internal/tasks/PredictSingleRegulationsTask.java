package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
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
import cz.cas.mbu.genexpi.compute.RegulationType;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

@RememberAllValues
public class PredictSingleRegulationsTask extends AbstractTask {
	
	@Tunable(description="Network")
	public ListSingleSelection<CyNetwork> network;
	
	@Tunable(description="Column containing mapping to expression time series",listenForChange="network")
	public ListSingleSelection<String> getExpressionTimeSeriesColumn()
	{
		updateColumnSelection();
		return expressionTimeSeriesColumn;
	}
	
	@Tunable(description="Name of the time series for predicted profiles")
	public String resultsName;
	
	@Tunable(description="Store fit parameters in columns of the edge table")
	public boolean storeParametersInEdgeTable = true;
	
	@Tunable(description="Prefix for column names of parameters", dependsOn="storeParametersInEdgeTable=true")
	public String parametersPrefix;
	
	@Tunable(description="Force prediction even for nodes marked as not useful")
	public boolean forcePrediction;
	
	@ContainsTunables
	public ErrorDef errorDef;
	
	@Tunable(description="Quality required for good fit (0-1)",groups={"Error"})
	public double requiredQuality = 0.8f;

	@Tunable(description="Regulations to consider")
	public RegulationType regulationType = RegulationType.PositiveOnly;

	
	private final CyServiceRegistrar registrar;
	
	private ListSingleSelection<String> expressionTimeSeriesColumn;
	
	
	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME); 
	
	public PredictSingleRegulationsTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		network = new ListSingleSelection<>(new ArrayList<>(registrar.getService(CyNetworkManager.class).getNetworkSet()));
		
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		CyNetwork currentNetwork = applicationManager.getCurrentNetwork();
		network.setSelectedValue(currentNetwork);

		expressionTimeSeriesColumn = new ListSingleSelection<>();
		updateColumnSelection();
		
		errorDef = ErrorDef.DEFAULT;
		
		registrar.getService(RememberValueService.class).loadProperties(this);		
		
	}

	private final void updateColumnSelection()
	{
		TaskUtils.updateSelectionOfTimeSeriesColumn(network, expressionTimeSeriesColumn, registrar);
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception
	{
		registrar.getService(RememberValueService.class).saveProperties(this);
		
		PredictionService predictionService = registrar.getService(PredictionService.class);
		
		try {
			predictionService.predictSingleRegulations(taskMonitor, network.getSelectedValue(), expressionTimeSeriesColumn.getSelectedValue(), resultsName, resultsName + "_Idx", storeParametersInEdgeTable, parametersPrefix, forcePrediction, errorDef, requiredQuality, regulationType);
		} catch (SuspectGPUResetByOSException ex)
		{
			UIUtils.handleSuspectedGPUResetInTask(registrar, ex);
		}
		
	}
	
	
}
