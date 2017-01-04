package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyEdge.Type;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.cydataseries.DataSeriesFactory;
import cz.cas.mbu.cydataseries.DataSeriesManager;
import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValue;
import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.IntegrateResults;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;

@RememberAllValues
public class MarkNoChangeGenesTask extends AbstractExpressionTask {

	
	@ContainsTunables
	public ErrorDef errorDef;
			
	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME); 
	
	public MarkNoChangeGenesTask(CyServiceRegistrar registrar) {
		super(registrar);
		errorDef = ErrorDef.DEFAULT;
		
		registrar.getService(RememberValueService.class).loadProperties(this);		
	}
	
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception
	{
		
		registrar.getService(RememberValueService.class).saveProperties(this);		
		
		PredictionService predictionService = registrar.getService(PredictionService.class);
		predictionService.markNoChangeGenes(network.getSelectedValue(), expressionTimeSeriesColumn.getSelectedValue(), errorDef);
						
	}

}
