package cz.cas.mbu.cygenexpi.internal;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.genexpi.compute.GeneProfile;

public class Utils {
	public static GeneProfile<Float> geneProfileFromRow(CyRow targetRow, TimeSeries expressionSeries, String timeSeriesColumnName)
	{							
			Integer targetTSId = targetRow.get(timeSeriesColumnName, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
			
			if(targetTSId == null)
			{					
				return null;
			}
			else
			{
				return geneProfileFromTSRow(expressionSeries, targetTSId);
			}
	}
	
	public static GeneProfile<Float> geneProfileFromTSRow(TimeSeries expressionSeries, int rowId)
	{
		List<Float> floatProfile = expressionSeries.getRowData(rowId).stream()
				.map(x ->
				{
					if(x < 0 || Double.isNaN(x))
					{
						return (float)0;
					}
					else
					{
						return x.floatValue(); 
					}
				})
				.collect(Collectors.toList());
		return new GeneProfile<>(expressionSeries.getRowName(rowId), floatProfile);					
		
	}
	
}
