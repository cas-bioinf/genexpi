package cz.cas.mbu.cygenexpi.internal.tasks;

import java.util.ArrayList;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.PredictionService;

public abstract class AbstractExpressionTask extends AbstractValidatedTask {

	@Tunable(description = "Network", gravity = 0)
	public ListSingleSelection<CyNetwork> network;

	@Tunable(description = "Column containing mapping to expression time series", listenForChange = "network", gravity = 1)
	public ListSingleSelection<String> getExpressionTimeSeriesColumn() {
		updateColumnSelection();
		return expressionTimeSeriesColumn;
	}

	public void setExpressionTimeSeriesColumn(ListSingleSelection<String> expressionTimeSeriesColumn) {
		this.expressionTimeSeriesColumn = expressionTimeSeriesColumn;
	}

	protected final CyServiceRegistrar registrar;
	
	private boolean updatingColumnSelection = false;
	protected ListSingleSelection<String> expressionTimeSeriesColumn;

	public AbstractExpressionTask(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
		
		network = new ListSingleSelection<>(new ArrayList<>(registrar.getService(CyNetworkManager.class).getNetworkSet()));
		
		CyApplicationManager applicationManager = registrar.getService(CyApplicationManager.class);
		CyNetwork currentNetwork = applicationManager.getCurrentNetwork();
		network.setSelectedValue(currentNetwork);

		expressionTimeSeriesColumn = new ListSingleSelection<>();
		updateColumnSelection();
		
	}

	protected final void updateColumnSelection() {
		if(!updatingColumnSelection)
		{
			try {
				updatingColumnSelection = true;
				TaskUtils.updateSelectionOfTimeSeriesColumn(network, expressionTimeSeriesColumn, registrar);
			}
			finally {
				updatingColumnSelection = false;
			}
		}
	}

	@Override
	protected ValidationState getValidationState(StringBuilder messageBuilder) {
		if(TaskUtils.isNoColumnAvailable(expressionTimeSeriesColumn))
		{
			messageBuilder.append("There is no time series mapped for the selected network.");
			return ValidationState.INVALID;
		}
		if(network.getSelectedValue() == null)
		{
			messageBuilder.append("You have to select a network.");
			return ValidationState.INVALID;
		}
		if(expressionTimeSeriesColumn.getSelectedValue() == null || expressionTimeSeriesColumn.getSelectedValue().isEmpty())
		{
			messageBuilder.append("You have to select an expression column.");
			return ValidationState.INVALID;			
		}
		
		TimeSeries expressionSeries = registrar.getService(DataSeriesMappingManager.class).getMappedDataSeries(network.getSelectedValue(), CyNode.class, expressionTimeSeriesColumn.getSelectedValue(), TimeSeries.class);
		if(expressionSeries == null)
		{
			messageBuilder.append("No valid time series is mapped to column '" + expressionTimeSeriesColumn.getSelectedValue() + "' of network '" + network.getSelectedValue() + "'");
			return ValidationState.INVALID;
		}
		
		
		return registrar.getService(PredictionService.class).validateTimeSeriesForPrediction(expressionSeries, messageBuilder);
	}

}