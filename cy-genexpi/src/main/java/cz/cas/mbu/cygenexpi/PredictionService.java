package cz.cas.mbu.cygenexpi;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;

public interface PredictionService {

	/**
	 * Adds "no change" tag to nodes that have no change with respect to the given errorDef. 
	 * It also erases the "no change" tag from the nodes that do change. Other tags of nodes that have change are kept intact.
	 * @param selectedNetwork
	 * @param expressionTimeSeriesColumn
	 * @param errorDef
	 */
	void markNoChangeGenes(CyNetwork selectedNetwork, String expressionTimeSeriesColumn, ErrorDef errorDef);

	/**
	 * Adds "constant synthesis" tag to nodes that can be modelled with constant synthesis to a good enough extent. 
	 * It also erases the "constant synthesis" tag from the nodes that cannot be modelled. Other tags of nodes that cannot be modelled are kept intact.
	 * Nodes tagged with {@link ProfileTags#NO_CHANGE} are ignored.
	 * @param taskMonitor
	 * @param selectedNetwork
	 * @param expressionTimeSeriesColumn
	 * @param errorDef
	 * @param requiredQuality
	 * @param storeFitsInTimeSeries
	 * @param resultsName
	 * @param resultsMappingColumnName
	 * @param storeParametersInNodeTable
	 * @param parametersPrefix
	 */
	void markConstantSynthesis(TaskMonitor taskMonitor, CyNetwork selectedNetwork, String expressionTimeSeriesColumn,
			ErrorDef errorDef, double requiredQuality, boolean storeFitsInTimeSeries, String resultsName, 
			String resultsMappingColumnName, boolean storeParametersInNodeTable, String parametersPrefix);

	/**
	 * Changes all profile tags for edges.
	 * @param taskMonitor
	 * @param selectedNetwork
	 * @param expressionTimeSeriesColumn
	 * @param resultsName
	 * @param storeParametersInEdgeTable
	 * @param parametersPrefix
	 * @param forcePrediction
	 * @param errorDef
	 * @param requiredQuality
	 */
	void predictSingleRegulations(TaskMonitor taskMonitor, CyNetwork selectedNetwork, String expressionTimeSeriesColumn,
			String resultsSeriesName, String resultsColumnName, boolean storeParametersInEdgeTable, String parametersPrefix, boolean forcePrediction,
			ErrorDef errorDef, double requiredQuality);
	
	ValidationState validateTimeSeriesForPrediction(TimeSeries expressionSeries, StringBuilder message);

}