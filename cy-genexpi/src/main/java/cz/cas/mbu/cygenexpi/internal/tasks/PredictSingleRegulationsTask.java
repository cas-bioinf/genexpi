package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.cygenexpi.internal.ui.UIUtils;
import cz.cas.mbu.genexpi.compute.RegulationType;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

@RememberAllValues
public class PredictSingleRegulationsTask extends AbstractExpressionTask {
	
	@Tunable(description="Name of the time series for predicted profiles", gravity = 2)
	public String resultsName;
	
	@Tunable(description="Store fit parameters in columns of the edge table", gravity = 3)
	public boolean storeParametersInEdgeTable = true;
	
	@Tunable(description="Prefix for column names of parameters", dependsOn="storeParametersInEdgeTable=true", gravity = 4)
	public String parametersPrefix;
	
	@Tunable(description="Force prediction even for nodes marked as not useful", gravity = 5)
	public boolean forcePrediction;
	
	@ContainsTunables
	public ErrorDef errorDef;
	
	@Tunable(description="Quality required for good fit (0-1)",groups={"Error"})
	public double requiredQuality = 0.8f;

	@Tunable(description="Regulations to consider", gravity = 6)
	public ListSingleSelection<RegulationType> regulationType;

	
	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME); 
	
	public PredictSingleRegulationsTask(CyServiceRegistrar registrar) {
		super(registrar);
				
		errorDef = ErrorDef.DEFAULT;
		
		regulationType = new ListSingleSelection<>(RegulationType.values());
		regulationType.setSelectedValue(RegulationType.PositiveOnly);
		
		registrar.getService(RememberValueService.class).loadProperties(this);		
		
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception
	{
		registrar.getService(RememberValueService.class).saveProperties(this);
		
		PredictionService predictionService = registrar.getService(PredictionService.class);
		
		try {
			predictionService.predictSingleRegulations(taskMonitor, network.getSelectedValue(), expressionTimeSeriesColumn.getSelectedValue(), resultsName, resultsName + "_Idx", storeParametersInEdgeTable, parametersPrefix, forcePrediction, errorDef, requiredQuality, regulationType.getSelectedValue());
		} catch (SuspectGPUResetByOSException ex)
		{
			UIUtils.handleSuspectedGPUResetInTask(registrar, ex);
		}
		
	}
	
	
}
