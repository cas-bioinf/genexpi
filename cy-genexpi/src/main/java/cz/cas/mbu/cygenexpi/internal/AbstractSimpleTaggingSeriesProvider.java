package cz.cas.mbu.cygenexpi.internal;

import org.cytoscape.model.CyRow;

import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;

public abstract class AbstractSimpleTaggingSeriesProvider  implements TaggingSeriesProvider {
	
	private final MappingDescriptor<TimeSeries> mainSeriesDescriptor;
			
	public AbstractSimpleTaggingSeriesProvider(MappingDescriptor<TimeSeries> mainSeriesDescriptor) {
		super();
		this.mainSeriesDescriptor = mainSeriesDescriptor;
	}

	@Override
	public boolean isRelevant(CyRow taggedRow) {
		int row = mainSeriesDescriptor.getDataSeriesRow(taggedRow);
		return row >= 0;
	}
	
	@Override
	public String getTaggingTitle(CyRow taggedRow) {
		int row = mainSeriesDescriptor.getDataSeriesRow(taggedRow);
		if(row >= 0)
		{
			return mainSeriesDescriptor.getDataSeries().getRowName(row);
		}
		else
		{
			return "No relevant profile";						
		}
	}

}
