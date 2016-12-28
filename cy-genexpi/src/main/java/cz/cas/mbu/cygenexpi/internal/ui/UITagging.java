package cz.cas.mbu.cygenexpi.internal.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.AbstractSimpleTaggingSeriesProvider;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.cygenexpi.internal.TaggingInfo;
import cz.cas.mbu.cygenexpi.internal.TaggingSeriesDescriptor;
import cz.cas.mbu.cygenexpi.internal.TaggingSeriesProvider;

public class UITagging {
	
	public static BatchTaggingPanel startNodeProfilesTagging(CyServiceRegistrar registrar, CyNetwork network, String expressionTimeSeriesColumn, ErrorDef errorDef, String noRegulatorTimeSeriesColumn, boolean reset, boolean resetExcluded)
	{
		if(reset)
		{
			TaggingService taggingService = registrar.getService(TaggingService.class);
			network.getDefaultEdgeTable().getAllRows().forEach(row ->
			{
				if(resetExcluded || !taggingService.getHumanApprovalTag(row).equals(HumanApprovalTags.EXCLUDED))
				{
					taggingService.setHumanApprovalTag(row, HumanApprovalTags.NO_TAG);
				}
			});
		}
		
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
		
		TimeSeries expressionSeries = mappingManager.getMappedDataSeries(network, CyNode.class, expressionTimeSeriesColumn, TimeSeries.class);
		final MappingDescriptor<TimeSeries> expressionSeriesDescriptor = new MappingDescriptor<TimeSeries>(network, CyNode.class, expressionTimeSeriesColumn, expressionSeries);
		
		TaggingSeriesProvider seriesProvider;
		
		if(noRegulatorTimeSeriesColumn == null)
		{
			seriesProvider = new AbstractSimpleTaggingSeriesProvider(expressionSeriesDescriptor) {
				@Override
				public List<TaggingSeriesDescriptor> getSeriesForTagging(CyRow taggedRow) {
					return Collections.singletonList(new TaggingSeriesDescriptor(expressionSeriesDescriptor, taggedRow, true));
				}
			}; 
		}
		else
		{
			TimeSeries noRegulatorSeries = mappingManager.getMappedDataSeries(network, CyNode.class, noRegulatorTimeSeriesColumn, TimeSeries.class);
			final MappingDescriptor<TimeSeries> noRegulatorSeriesDescriptor = new MappingDescriptor<TimeSeries>(network, CyNode.class, noRegulatorTimeSeriesColumn, noRegulatorSeries);
			
			seriesProvider = new AbstractSimpleTaggingSeriesProvider(expressionSeriesDescriptor) {
				@Override
				public List<TaggingSeriesDescriptor> getSeriesForTagging(CyRow taggedRow) {
					return Arrays.asList(new TaggingSeriesDescriptor(expressionSeriesDescriptor, taggedRow, true), new TaggingSeriesDescriptor(noRegulatorSeriesDescriptor, taggedRow, false));
				}
			}; 
				
				
		}
				
		
		String[] possibleTags = new String[] {ProfileTags.NO_TAG, ProfileTags.NO_CHANGE, ProfileTags.CONSTANT_SYNTHESIS }; 
		TaggingInfo info = new TaggingInfo(possibleTags, "Useful profile", seriesProvider, errorDef);
		BatchTaggingPanel taggingPanel = new BatchTaggingPanel(registrar, info, TaggingMode.NODE, "Tagging nodes");
		
		
		registrar.registerService(taggingPanel, CytoPanelComponent.class, new Properties());
		
		UIUtils.ensurePanelVisible(registrar, taggingPanel);		
			
		return taggingPanel;
	}
	
	public static BatchTaggingPanel startPredictedEdgeProfilesTagging(CyServiceRegistrar registrar, CyNetwork network, String expressionTimeSeriesColumn, ErrorDef errorDef, String predictionTimeSeriesColumn, boolean reset, boolean resetExcluded)
	{
		if(reset)
		{
			TaggingService taggingService = registrar.getService(TaggingService.class);
			network.getDefaultEdgeTable().getAllRows().forEach(row ->
			{
				if(resetExcluded || !taggingService.getHumanApprovalTag(row).equals(HumanApprovalTags.EXCLUDED))
				{
					taggingService.setHumanApprovalTag(row, HumanApprovalTags.NO_TAG);
				}
			});
		}
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class);
		
		TimeSeries expressionSeries = mappingManager.getMappedDataSeries(network, CyNode.class, expressionTimeSeriesColumn, TimeSeries.class);
		final MappingDescriptor<TimeSeries> expresssionSeriesDescriptor = new MappingDescriptor<TimeSeries>(network, CyNode.class, expressionTimeSeriesColumn, expressionSeries);
		
		TimeSeries predictionSeries = mappingManager.getMappedDataSeries(network, CyEdge.class, predictionTimeSeriesColumn, TimeSeries.class);
		final MappingDescriptor<TimeSeries> predictionSeriesDescriptor = new MappingDescriptor<TimeSeries>(network, CyEdge.class, predictionTimeSeriesColumn, predictionSeries);

		TaggingSeriesProvider taggingSeriesProvider = new AbstractSimpleTaggingSeriesProvider(predictionSeriesDescriptor) {
			
			@Override
			public List<TaggingSeriesDescriptor> getSeriesForTagging(CyRow taggedRow) {
				List<TaggingSeriesDescriptor> result = new ArrayList<>();
				
				CyNetwork network = registrar.getService(CyApplicationManager.class).getCurrentNetwork();

				CyEdge edge = network.getEdge(taggedRow.get(CyIdentifiable.SUID, Long.class));
				CyTable nodeTable = network.getDefaultNodeTable();
				result.add(new TaggingSeriesDescriptor(expresssionSeriesDescriptor, nodeTable.getRow(edge.getTarget().getSUID()), true)); //show error only for the prediction target
				result.add(new TaggingSeriesDescriptor(expresssionSeriesDescriptor, nodeTable.getRow(edge.getSource().getSUID()), false));
				
				result.add(new TaggingSeriesDescriptor(predictionSeriesDescriptor, taggedRow, false));
				return result;			
			}
		};									
			
		
		String[] possibleTags = new String[] { ProfileTags.NO_FIT, ProfileTags.BORDERLINE_FIT, ProfileTags.GOOD_FIT }; 
		TaggingInfo info = new TaggingInfo(possibleTags, "<Unprocessed>", taggingSeriesProvider, errorDef);
		final BatchTaggingPanel taggingPanel = new BatchTaggingPanel(registrar,info, TaggingMode.EDGE, "Tagging edges");		
				
		registrar.registerService(taggingPanel, CytoPanelComponent.class, new Properties());
		
		UIUtils.ensurePanelVisible(registrar, taggingPanel);
		
		return taggingPanel;
	}
}
