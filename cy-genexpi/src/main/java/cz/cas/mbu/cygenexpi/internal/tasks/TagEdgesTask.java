package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValue;
import cz.cas.mbu.cygenexpi.RememberValueService;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.AbstractSimpleTaggingSeriesProvider;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.cygenexpi.internal.TaggingInfo;
import cz.cas.mbu.cygenexpi.internal.TaggingSeriesDescriptor;
import cz.cas.mbu.cygenexpi.internal.TaggingSeriesProvider;
import cz.cas.mbu.cygenexpi.internal.ui.BatchTaggingPanel;
import cz.cas.mbu.cygenexpi.internal.ui.TaggingMode;
import cz.cas.mbu.cygenexpi.internal.ui.UITagging;
import cz.cas.mbu.cygenexpi.internal.ui.UIUtils;

@RememberAllValues
public class TagEdgesTask extends AbstractValidatedTask {
	
	private final CyServiceRegistrar registrar;
	
	@Tunable(description="Column containing mapping to expression time series")
	public ListSingleSelection<String> expressionTimeSeriesColumn;
	
	@Tunable(description="Column containing mapping to predicted profiles")
	public ListSingleSelection<String> predictionTimeSeriesColumn;
	
	@ContainsTunables
	public ErrorDef errorDef;
	
	@Tunable(description="Reset all approval tags")
	public boolean reset;

	@Tunable(description="Also reset approval tags for excluded profiles",dependsOn="reset=true")
	public boolean resetExcluded;
	
	private final CyNetwork selectedNetwork;
	
	public TagEdgesTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		selectedNetwork = applicationManager.getCurrentNetwork();
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);		
		CyTable nodeTable = mappingManager.getMappingTable(selectedNetwork, CyNode.class); 
		CyTable edgeTable = mappingManager.getMappingTable(selectedNetwork, CyEdge.class); 
		
		Map<String, TimeSeries> nodeMappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		
		List<String> possibleExpressionColumns = nodeMappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());
		
		expressionTimeSeriesColumn = new ListSingleSelection<>(possibleExpressionColumns);

		Map<String, TimeSeries> edgeMappings = mappingManager.getAllMappings(selectedNetwork, CyEdge.class, TimeSeries.class);
		List<String> possiblePredictionColumns = edgeMappings.keySet().stream()
				.filter(col -> (edgeTable.getColumn(col) != null))
				.collect(Collectors.toList());
		
		predictionTimeSeriesColumn = new ListSingleSelection<>(possiblePredictionColumns);
		
		errorDef = new ErrorDef(0f, 0.2f);
		
		registrar.getService(RememberValueService.class).loadProperties(this);		
		
	}

	@Override
	protected ValidationState getValidationState(StringBuilder messageBuilder) {
		if(predictionTimeSeriesColumn.getSelectedValue().equals( expressionTimeSeriesColumn.getSelectedValue()))
		{
			messageBuilder.append("Expression time series and predicted time series must be different");
			return ValidationState.INVALID;
		}
		return ValidationState.OK;
	}


	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		registrar.getService(RememberValueService.class).saveProperties(this);		

		UITagging.startPredictedEdgeProfilesTagging(registrar, selectedNetwork, expressionTimeSeriesColumn.getSelectedValue(), errorDef, predictionTimeSeriesColumn.getSelectedValue(), reset, resetExcluded);			
	}

}
