package cz.cas.mbu.cygenexpi.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.opencl.cycl.CyCL;
import org.cytoscape.opencl.cycl.CyCLDevice;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import cz.cas.mbu.cydataseries.DataSeriesFactory;
import cz.cas.mbu.cydataseries.DataSeriesManager;
import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.EErrorFunction;
import cz.cas.mbu.genexpi.compute.ELossFunction;
import cz.cas.mbu.genexpi.compute.EMethod;
import cz.cas.mbu.genexpi.compute.GNCompute;
import cz.cas.mbu.genexpi.compute.GNException;
import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.InferenceModel;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.IntegrateResults;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

public class PredictionServiceImpl implements PredictionService {

	private static final int MAX_TASKS_PER_EXECUTION = 2048;
private final CyServiceRegistrar registrar;
	
	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME); 
			
	
	public PredictionServiceImpl(CyServiceRegistrar registrar) {
		super();
		this.registrar = registrar;
	}

	protected CLDevice getDeviceFromCyCLPreferred()
	{	
		//TODO once 3.5.0 is out, revert to the more nice code
		/*
		CyCLDevice cyCLPreferredDevice = CyCL.getPreferredDevice();
		if(cyCLPreferredDevice == null)
		{
			return null;
		}
		*/

		if (CyCL.getDevices().isEmpty())
		{
			return null;
		}
		
		CyCLDevice cyCLPreferredDevice = CyCL.getDevices().get(0);
		
		//TODO once 3.5.0 is out, revert to the more nice code
		//String cyCLPreferredName = cyCLPreferredDevice.openCLName;
		String cyCLPreferredName = cyCLPreferredDevice.name;
		String cyCLPreferredVersion = cyCLPreferredDevice.version;

		CLDevice bestDevice = null;
		for(CLPlatform platform : JavaCL.listPlatforms())
		{
			for(CLDevice device : platform.listAllDevices(false))
			{
				String javaCLName = device.getName();
				if (cyCLPreferredName.equals(javaCLName) && cyCLPreferredVersion.equals(device.getVersion()))
				{
					//full match, immediately return
					return device;
				}
				//Check only for a name match
				else if (cyCLPreferredName.equals(javaCLName) && (bestDevice == null || !cyCLPreferredName.equals(bestDevice.getName())))
				{
					bestDevice = device;
				}
				//Check if tha JavaCL name is the last part of the CyCL name (lowest priority)
				else if (cyCLPreferredName.endsWith(javaCLName))
				{
					if(bestDevice == null)
					{
						bestDevice = device;
					}
					//Override the best device only if it was a partial match and I have a version match
					else if (!cyCLPreferredName.equals(bestDevice.getName()) && cyCLPreferredVersion.equals(device.getVersion())) 
					{
						bestDevice = device;
					}
				}
			}
		}
		
		return bestDevice;
	}
	
	protected CLContext getContext()
	{
		CLDevice cyClPreferred = getDeviceFromCyCLPreferred();
		
		if(cyClPreferred != null)
		{
			return cyClPreferred.getPlatform().createContext(Collections.EMPTY_MAP,cyClPreferred);
		}
		else
		{
			CLContext context = GNCompute.getBestContext();
			if(context == null)
			{
				throw new GNException("No OpenCL context could be created. You may need to install OpenCL drivers for your processor/GPU.");
			}
			userLogger.info("Using context: " + context.toString());
			return context;
		}
	}
	
	/* (non-Javadoc)
	 * @see cz.cas.mbu.genexpi.internal.PredictionService#markNoChangeGenes(org.cytoscape.model.CyNetwork, java.lang.String, cz.cas.mbu.cygngpu.internal.ErrorDef)
	 */
	@Override
	public void markNoChangeGenes(CyNetwork selectedNetwork, String expressionTimeSeriesColumn, ErrorDef errorDef)
	{
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
		TimeSeries expressionSeries = mappings.get(expressionTimeSeriesColumn);
		
		if(expressionSeries == null)
		{
			throw new GNException("Could not find any time series associated with node table column '" + expressionTimeSeriesColumn);
		}
		
		TaggingService taggingService = registrar.getService(TaggingService.class);
		
		CyTable nodeTable = selectedNetwork.getDefaultNodeTable();
		
		selectedNetwork.getNodeList().stream()
			.forEach(node ->
			{
				CyRow targetRow = nodeTable.getRow(node.getSUID());
				Integer targetTSId = targetRow.get(expressionTimeSeriesColumn, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
				if(targetTSId == null)
				{
					return;
				}
				List<Double> profile = expressionSeries.getRowData(targetTSId);
				double minimum = profile.stream().reduce(Double.POSITIVE_INFINITY,Math::min);
				double maximum = profile.stream().reduce(Double.NEGATIVE_INFINITY,Math::max);
				
				double maximumLowErrorBar  = maximum - errorDef.getErrorMargin(maximum);
				double minimumHighErrorBar = minimum + errorDef.getErrorMargin(minimum);

				if(maximumLowErrorBar <= minimumHighErrorBar)
				{
					taggingService.setProfileTag(targetRow, ProfileTags.NO_CHANGE);
					taggingService.setHumanApprovalTag(targetRow, HumanApprovalTags.NO_TAG);
				}
				else if(taggingService.getProfileTag(targetRow).equals(ProfileTags.NO_CHANGE)) 
				{
					taggingService.setProfileTag(targetRow, ProfileTags.NO_TAG);
					taggingService.setHumanApprovalTag(targetRow, HumanApprovalTags.NO_TAG);
				}
										
			});
	}
	
	/* (non-Javadoc)
	 * @see cz.cas.mbu.genexpi.internal.PredictionService#markConstantSynthesis(org.cytoscape.work.TaskMonitor, org.cytoscape.model.CyNetwork, java.lang.String, cz.cas.mbu.cygngpu.internal.ErrorDef, double, boolean, java.lang.String, boolean, java.lang.String)
	 */
	@Override
	public void markConstantSynthesis(TaskMonitor taskMonitor, CyNetwork selectedNetwork, String expressionTimeSeriesColumn, ErrorDef errorDef, double requiredQuality, boolean storeFitsInTimeSeries, String resultsName, String resultsMappingColumnName, boolean storeParametersInNodeTable, String parametersPrefix)
	{
		JavaCLHelper.runWithCLClassloader(() -> {
		
			try {
				taskMonitor.setStatusMessage("Gathering data");
				
				CyTable nodeTable = selectedNetwork.getDefaultNodeTable();
				
				DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
				Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
				TimeSeries expressionSeries = mappings.get(expressionTimeSeriesColumn);
				
				if(expressionSeries == null)
				{
					throw new GNException("Could not find any time series associated with node table column '" + expressionTimeSeriesColumn);
				}
				
				double timeStep = getTimeStep(expressionSeries);
				
				boolean useCustomTimeStep = Math.abs(timeStep - 1) > 0.0001;
				
				List<GeneProfile<Float>> geneProfiles = new ArrayList<>();
				List<NoRegulatorInferenceTask> inferenceTasks = new ArrayList<>();
				List<CyNode> nodesCorrespondingToInferenceTasks = new ArrayList<>();
				
				TaggingService taggingService = registrar.getService(TaggingService.class);
				
				selectedNetwork.getNodeList().stream()
					.forEach(node ->
					{
						CyRow targetRow = nodeTable.getRow(node.getSUID());
						GeneProfile<Float> profile = Utils.geneProfileFromRow(targetRow,expressionSeries,expressionTimeSeriesColumn);
						
						String nodeTag = taggingService.getProfileTag(targetRow);
						if(nodeTag.equalsIgnoreCase(ProfileTags.NO_CHANGE))
						{
							CyColumn mappingColumn = nodeTable.getColumn(resultsMappingColumnName);
							if(mappingColumn != null)
							{
								targetRow.set(resultsMappingColumnName, null);								
							}
							return;
						}
						
						if(profile == null)
						{
							String message = "Could not test regulation for node SUID " + node.getSUID() + " as it containes no value for mapping column " + expressionTimeSeriesColumn;
							userLogger.warn(message);
						}
						else
						{
							int targetProfileIndex = geneProfiles.size();
							geneProfiles.add(profile);					
							
							inferenceTasks.add(new NoRegulatorInferenceTask(targetProfileIndex));
							nodesCorrespondingToInferenceTasks.add(node);
						}
					});
				
				
				InferenceModel model = InferenceModel.NO_REGULATOR;
				EMethod method = EMethod.Annealing;
				ELossFunction lossFunction = ELossFunction.Squared;
				float regularizationWeight = 24;
				
				CLContext context = getContext();
				taskMonitor.setStatusMessage("Predicting, using " + context.getDevices()[0].getName() +  "\n(if running on a GPU, computer may become unresponsive)");
				GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, method, null /*No error function*/, lossFunction, regularizationWeight, useCustomTimeStep, (float)timeStep);
				
				int numIterations = 128;
				//TODO once 3.5.0 is out, revert to the more nice code
				//List<InferenceResult> results = compute.computeNoRegulator(geneProfiles, inferenceTasks, numIterations, CyCL.isPreventFullOccupation());
				List<InferenceResult> results = compute.computeNoRegulator(geneProfiles, inferenceTasks, numIterations, false);
				
			
				//Calculate the best profiles + error rate
				double[][] resultsData = new double[results.size()][geneProfiles.get(0).getProfile().size()]; 
				double[] timePoints = Arrays.copyOf(expressionSeries.getIndexArray(), expressionSeries.getIndexArray().length);		
				for(int taskId = 0; taskId < inferenceTasks.size(); taskId++)
				{
					taskMonitor.setStatusMessage("Processing results [" + taskId + "/" + inferenceTasks.size() + "]");
					taskMonitor.setProgress((double)taskId / (double)inferenceTasks.size());
					
					NoRegulatorInferenceTask task = inferenceTasks.get(taskId);
					double[] predictedProfile = IntegrateResults.integrateNoRegulator(results.get(taskId), task, geneProfiles, timePoints[0], timePoints);
					for(int t = 0; t < predictedProfile.length; t++)
					{
						resultsData[taskId][t] = predictedProfile[t];
					}			
					
					//If the fit is good, mark the node
					double quality = FitMetrics.fitQuality(geneProfiles.get(taskId), errorDef, predictedProfile);				
					CyNode node = nodesCorrespondingToInferenceTasks.get(taskId);
					CyRow targetRow = nodeTable.getRow(node.getSUID());

					if(quality >= requiredQuality)
					{
						taggingService.setProfileTag(targetRow, ProfileTags.CONSTANT_SYNTHESIS);
						taggingService.setHumanApprovalTag(targetRow, HumanApprovalTags.NO_TAG);
					}
					else if(taggingService.getProfileTag(targetRow).equals(ProfileTags.CONSTANT_SYNTHESIS))
					{
						taggingService.setProfileTag(targetRow, ProfileTags.NO_TAG);
						taggingService.setHumanApprovalTag(targetRow, HumanApprovalTags.NO_TAG);						
					}
				}
	
				if(storeFitsInTimeSeries)
				{
					List<String> rowNames = new ArrayList<>();
					for(int taskId = 0; taskId < inferenceTasks.size(); taskId++)
					{
						NoRegulatorInferenceTask task = inferenceTasks.get(taskId);
						rowNames.add(geneProfiles.get(task.getTargetID()).getName() + " Const. reg");
					}
					
					TimeSeries resultsSeries = registrar.getService(DataSeriesFactory.class).createTimeSeries(resultsName, rowNames, timePoints, resultsData);
					registrar.getService(DataSeriesManager.class).registerDataSeries(resultsSeries);			
					if(nodeTable.getColumn(resultsMappingColumnName) == null)
					{
						nodeTable.createColumn(resultsMappingColumnName, DataSeriesMappingManager.MAPPING_COLUMN_CLASS, false);
					}
					
					registrar.getService(DataSeriesMappingManager.class).mapDataSeriesRowsToTableColumn(selectedNetwork, CyNode.class, resultsMappingColumnName, resultsSeries);
	
					for(int resultIdx = 0; resultIdx < inferenceTasks.size(); resultIdx++)
					{
						CyRow row = nodeTable.getRow(nodesCorrespondingToInferenceTasks.get(resultIdx).getSUID());
						row.set(resultsMappingColumnName, resultsSeries.getRowID(resultIdx));									
					}
					
				}
				
				
				if(storeParametersInNodeTable)
				{
					String errorColumnName = parametersPrefix + "error";
					
					for(String param: model.getParameterNames())
					{
						String columnName = parametersPrefix + param;
						if(nodeTable.getColumn(columnName) == null)
						{
							nodeTable.createColumn(columnName, Double.class, false);
						}
					}
					if(nodeTable.getColumn(errorColumnName) == null)
					{
						nodeTable.createColumn(errorColumnName, Double.class, false);
					}
					
					for(int resultIdx = 0; resultIdx < inferenceTasks.size(); resultIdx++)
					{
						CyRow row = nodeTable.getRow(nodesCorrespondingToInferenceTasks.get(resultIdx).getSUID());
	
						for(int paramId = 0; paramId < model.getNumParams(); paramId++)
						{
							row.set(parametersPrefix + model.getParameterNames()[paramId], results.get(resultIdx).getParameters()[paramId]);				
						}
						row.set(errorColumnName, results.get(resultIdx).getError());										
					}				
				}
							
			}
			catch(GNException ex)
			{
				userLogger.error("Could not predict expression.", ex);
				throw ex;
			}
			catch(Exception ex)
			{
				userLogger.error("Could not predict expression.", ex);		
				throw new GNException(ex);
			}
			catch(Error e)
			{
				userLogger.error("Error during prediction.", e);
				throw e;
			}
		});
	}
	
	
	/* (non-Javadoc)
	 * @see cz.cas.mbu.genexpi.internal.PredictionService#predictSingleRegulations(org.cytoscape.work.TaskMonitor, org.cytoscape.model.CyNetwork, java.lang.String, java.lang.String, boolean, java.lang.String, boolean, cz.cas.mbu.cygngpu.internal.ErrorDef, double)
	 */
	@Override
	public void predictSingleRegulations(TaskMonitor taskMonitor, CyNetwork selectedNetwork,  String expressionTimeSeriesColumn, String resultsSeriesName, String resultsColumnName,  boolean storeParametersInEdgeTable, String parametersPrefix, boolean forcePrediction,ErrorDef errorDef, double requiredQuality)
	{
		JavaCLHelper.runWithCLClassloader(() -> {
		
			try {
				taskMonitor.setStatusMessage("Gathering data");
				
				DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
				Map<String, TimeSeries> mappings = mappingManager.getAllMappings(selectedNetwork, CyNode.class, TimeSeries.class);
				TimeSeries expressionSeries = mappings.get(expressionTimeSeriesColumn);

				if(expressionSeries == null)
				{
					throw new GNException("Could not find any time series associated with node table column '" + expressionTimeSeriesColumn);
				}
				
				double timeStep = getTimeStep(expressionSeries);
				
				boolean useCustomTimeStep = Math.abs(timeStep - 1) > 0.0001;
				
				
				List<GeneProfile<Float>> geneProfiles = new ArrayList<>();
				Map<Integer, Integer> timeSeriesRowIndexToProfileIndex = new HashMap<>();
				List<AdditiveRegulationInferenceTask> inferenceTasks = new ArrayList<>();
				List<CyEdge> edgesCorrespondingToInferenceTasks = new ArrayList<>();
				List<Boolean> edgeExcluded = new ArrayList<>();
				
				TaggingService taggingService = registrar.getService(TaggingService.class);
				
				CyTable nodeTable = selectedNetwork.getDefaultNodeTable();
				CyTable edgeTable = selectedNetwork.getDefaultEdgeTable();
				
				selectedNetwork.getEdgeList().stream()
					.forEach(edge ->
					{
						CyRow edgeRow = edgeTable.getRow(edge.getSUID());
						CyRow regulatorRow = nodeTable.getRow(edge.getSource().getSUID());
						CyRow targetRow = nodeTable.getRow(edge.getTarget().getSUID());
					
						
						//Exclude profiles that are marked as "no change" or "constant synthesis"
						boolean excluded = false;
						String targetTag = taggingService.getProfileTag(targetRow);
						String sourceTag = taggingService.getProfileTag(regulatorRow);
						
						if(targetTag.equalsIgnoreCase(ProfileTags.NO_CHANGE))
						{
							taggingService.setProfileTag(edgeRow, ProfileTags.EXCLUDED_NO_CHANGE_TARGET);
							excluded = true;
						}
						else if(targetTag.equalsIgnoreCase(ProfileTags.CONSTANT_SYNTHESIS))
						{
							taggingService.setProfileTag(edgeRow, ProfileTags.EXCLUDED_CONSTANT_SYNTHESIS);
							excluded = true;
						}									
						else if(sourceTag.equalsIgnoreCase(ProfileTags.NO_CHANGE))
						{
							taggingService.setProfileTag(edgeRow, ProfileTags.EXCLUDED_NO_CHANGE_REGULATOR);
							excluded = true;						
						}
						
						if(excluded && !forcePrediction)
						{
							taggingService.setHumanApprovalTag(edgeRow, HumanApprovalTags.EXCLUDED);
							return;
						}
						
						edgeExcluded.add(excluded);
						
						Integer regulatorTSId = regulatorRow.get(expressionTimeSeriesColumn, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
						Integer targetTSId = targetRow.get(expressionTimeSeriesColumn, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
						
						if(regulatorTSId == null || targetTSId == null)
						{					
							String message = "Could not infer regulation for edge SUID " + edge.getSUID();
							if(regulatorTSId == null)
							{
								message += " the source node (SUID: " + edge.getSource().getSUID() + ") ";
							}
							if(targetTSId == null)
							{
								message += " the target node (SUID: " + edge.getSource().getSUID() + ") ";
							}
							message += "contains no value for mapping column " + expressionTimeSeriesColumn;
							userLogger.warn(message);
						}
						else
						{
							int regulatorProfileIndex;
							int targetProfileIndex;
							//get profile for regulator
							if(timeSeriesRowIndexToProfileIndex.containsKey(regulatorTSId))
							{
								regulatorProfileIndex = timeSeriesRowIndexToProfileIndex.get(regulatorTSId);
							}
							else
							{
								regulatorProfileIndex = geneProfiles.size();
								geneProfiles.add(Utils.geneProfileFromTSRow(expressionSeries, regulatorTSId));
								timeSeriesRowIndexToProfileIndex.put(regulatorTSId, regulatorProfileIndex);
							}
							
							//get profile for target
							if(timeSeriesRowIndexToProfileIndex.containsKey(targetTSId))
							{
								targetProfileIndex = timeSeriesRowIndexToProfileIndex.get(targetTSId);
							}
							else
							{
								targetProfileIndex = geneProfiles.size();
								geneProfiles.add(Utils.geneProfileFromTSRow(expressionSeries, targetTSId));
								timeSeriesRowIndexToProfileIndex.put(targetTSId, targetProfileIndex);
							}
							
							inferenceTasks.add(new AdditiveRegulationInferenceTask(regulatorProfileIndex, targetProfileIndex));
							edgesCorrespondingToInferenceTasks.add(edge);
						}
					});
				
				if(inferenceTasks.isEmpty())
				{
					return;
				}
				
				InferenceModel model = InferenceModel.createAdditiveRegulationModel(1);
				EMethod method = EMethod.Annealing;
				EErrorFunction errorFunction = EErrorFunction.Euler;
				ELossFunction lossFunction = ELossFunction.Squared;
				float regularizationWeight = 24;
				
				CLContext context = getContext();
				GNCompute<Float> compute = new GNCompute<>(Float.class, context, model, method, errorFunction, lossFunction, regularizationWeight, useCustomTimeStep, (float)timeStep);
				
				int numIterations = 128;
				//TODO once 3.5.0 is out, revert to the more nice code
				//List<InferenceResult> results = compute.computeAdditiveRegulation(geneProfiles, inferenceTasks, 1, numIterations, CyCL.isPreventFullOccupation());
				List<InferenceResult> results = new ArrayList<>();
				int numSteps = ((inferenceTasks.size() - 1) / MAX_TASKS_PER_EXECUTION) + 1;
				for(int step = 0; step < numSteps; step++)
				{
					taskMonitor.setStatusMessage("Predicting, using " + context.getDevices()[0].getName() +  "\n(if running on a GPU, computer may become unresponsive)\n" + (step * MAX_TASKS_PER_EXECUTION) + "/" + inferenceTasks.size());					
					taskMonitor.setProgress((double)step / (double)numSteps);
					int minIndex = step * MAX_TASKS_PER_EXECUTION;
					int maxIndex = Math.min((step + 1) * MAX_TASKS_PER_EXECUTION, inferenceTasks.size());
					List<InferenceResult> partialResults = compute.computeAdditiveRegulation(geneProfiles, inferenceTasks.subList(minIndex, maxIndex), 1, numIterations, false);
					results.addAll(partialResults);
				}
				
				
				List<String> rowNames = new ArrayList<>();
				double[] timePoints = Arrays.copyOf(expressionSeries.getIndexArray(), expressionSeries.getIndexArray().length);		
				double[][] resultsData = new double[results.size()][geneProfiles.get(0).getProfile().size()];
							
				for(int taskId = 0; taskId < inferenceTasks.size(); taskId++)
				{
					taskMonitor.setStatusMessage("Processing results [" + taskId + "/" + inferenceTasks.size() + "]");
					taskMonitor.setProgress((double)taskId / (double)inferenceTasks.size());
					AdditiveRegulationInferenceTask task = inferenceTasks.get(taskId);
					InferenceResult result = results.get(taskId);

					StringBuilder rowNameSuffix = new StringBuilder(" [");
					for(int paramId = 0; paramId < model.getNumParams(); paramId++)
					{
						if(paramId > 0)
						{
							rowNameSuffix.append(", ");
						}
						
						double paramValue = result.getParameters()[paramId];
						String formatStr;
						if(Math.abs(paramValue) >= 100)
						{
							formatStr = "%.0f";
						}
						else if(Math.abs(paramValue) > 10)
						{
							formatStr = "%.1f";							
						}
						else if(Math.abs(paramValue) > 1)
						{
							formatStr = "%.2f";							
						}
						else if(Math.abs(paramValue) < 0.001)
						{
							formatStr = "%f";							
						}
						else 
						{
							formatStr = "%.3f";
						}
						
						
						rowNameSuffix.append(model.getParameterNames()[paramId])
							.append("=")
							.append(String.format(formatStr, paramValue));
					}
					rowNameSuffix.append("]");
					
					rowNames.add(geneProfiles.get(task.getRegulatorIDs()[0]).getName() + " -> " + geneProfiles.get(task.getTargetID()).getName() + rowNameSuffix);
					double[] predictedProfile = IntegrateResults.integrateAdditiveRegulation(model, results.get(taskId), task, geneProfiles, timePoints[0], timeStep, timePoints);
					for(int t = 0; t < predictedProfile.length; t++)
					{
						resultsData[taskId][t] = predictedProfile[t];
					}
	
					//Tag the fits
					CyRow row = edgeTable.getRow(edgesCorrespondingToInferenceTasks.get(taskId).getSUID());
					double quality = FitMetrics.fitQuality(geneProfiles.get(inferenceTasks.get(taskId).getTargetID()), errorDef, predictedProfile);
	
					//Put tags only if not excluded from prior
					if(!edgeExcluded.get(taskId))
					{
						if(quality >= requiredQuality)
						{
							taggingService.setProfileTag(row, ProfileTags.GOOD_FIT);
						}
						else
						{
							taggingService.setProfileTag(row, ProfileTags.NO_FIT);						
						}					
					}
					
					taggingService.setHumanApprovalTag(row, HumanApprovalTags.NO_TAG);
					
				}
				
				TimeSeries resultsSeries = registrar.getService(DataSeriesFactory.class).createTimeSeries(resultsSeriesName, rowNames, timePoints, resultsData);
				registrar.getService(DataSeriesManager.class).registerDataSeries(resultsSeries);
				
				if(edgeTable.getColumn(resultsColumnName) == null)
				{
					edgeTable.createColumn(resultsColumnName, DataSeriesMappingManager.MAPPING_COLUMN_CLASS, false);
				}
				
				String errorColumnName = parametersPrefix + "error";
				if(storeParametersInEdgeTable)
				{
					for(String param: model.getParameterNames())
					{
						String columnName = parametersPrefix + param;
						if(edgeTable.getColumn(columnName) == null)
						{
							edgeTable.createColumn(columnName, Double.class, false);
						}
					}
					if(edgeTable.getColumn(errorColumnName) == null)
					{
						edgeTable.createColumn(errorColumnName, Double.class, false);
					}
				}
				
				registrar.getService(DataSeriesMappingManager.class).mapDataSeriesRowsToTableColumn(selectedNetwork, CyEdge.class, resultsColumnName, resultsSeries);
				
				for(int resultIdx = 0; resultIdx < inferenceTasks.size(); resultIdx++)
				{
					CyRow row = edgeTable.getRow(edgesCorrespondingToInferenceTasks.get(resultIdx).getSUID());
					row.set(resultsColumnName, resultsSeries.getRowID(resultIdx));
									
					if(storeParametersInEdgeTable)
					{
						for(int paramId = 0; paramId < model.getNumParams(); paramId++)
						{
							row.set(parametersPrefix + model.getParameterNames()[paramId], results.get(resultIdx).getParameters()[paramId]);				
						}
						row.set(errorColumnName, results.get(resultIdx).getError());					
					}
					
				}
			}
			catch(GNException ex)
			{
				userLogger.error("Could not predict expression.", ex);
				throw ex;
			}
			catch(Exception ex)
			{
				userLogger.error("Could not predict expression.", ex);		
				throw new GNException(ex);
			}
			catch (OutOfMemoryError e)
			{
				userLogger.error("Not enough memory for prediction.",e);
				throw new GNException("Not enough memory and/or OpenCL device memory for prediction.", e);
			}
			catch(Error e)
			{
				userLogger.error("Error during prediction.", e);
				throw e;
			}
		});
	}

	@Override
	public ValidationState validateTimeSeriesForPrediction(TimeSeries expressionSeries, StringBuilder message)
	{
		if(expressionSeries == null)
		{
			message.append("No time series was given.");
			return ValidationState.INVALID;
		}
		try {
			getTimeStep(expressionSeries);			
		}
		catch (Exception ex)
		{
			message.append(ex.getMessage());
			return ValidationState.INVALID;
		}
		
		return ValidationState.OK;
	}
	
	private double getTimeStep(TimeSeries expressionSeries) {
		double[] indexArray = expressionSeries.getIndexArray();

		if(indexArray.length < 2)
		{
			String error = "The expression time series needs at least 2 time points";
			userLogger.error(error);
			throw new GNException(error);
		}
		
		double timeStep = indexArray[1] - indexArray[0];
		if (timeStep <= 0.00001)
		{
			String error = "The time differences between all time points in the series have to be positive";
			userLogger.error(error);
			throw new GNException(error);
		}
		
		double allowedError = timeStep / 1000;
		for(int step = 2; step < indexArray.length; step++)
		{
			double currentTimeStep = indexArray[step] - indexArray[step - 1];
			if(Math.abs(currentTimeStep - timeStep) > allowedError)
			{
				String error = "The time differences between all time points in the series have to be equal.\n"
						+ " Difference between point " + step + " (time " + indexArray[step - 1] + ") and " + (step + 1) + " (time " + indexArray[step] + ")"
						+ " is not equal to the difference between 1st and 2nd point (" + timeStep + ").\n"
						+ "Consider smoothing/splining the series with the cy-dataseries plugin.";
				userLogger.error(error);
				throw new GNException(error);
			}
		}
		return timeStep;
	}
}
