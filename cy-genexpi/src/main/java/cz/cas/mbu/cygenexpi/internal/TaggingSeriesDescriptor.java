package cz.cas.mbu.cygenexpi.internal;

import org.cytoscape.model.CyRow;

import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaggingSeriesDescriptor {
	private MappingDescriptor<TimeSeries> mappingDescriptor;
	private CyRow row;
	boolean showError;
		
	
	public TaggingSeriesDescriptor(MappingDescriptor<TimeSeries> mappingDescriptor, CyRow row, boolean showError) {
		super();
		this.mappingDescriptor = mappingDescriptor;
		this.row = row;
		this.showError = showError;
	}
	
	public MappingDescriptor<TimeSeries> getMappingDescriptor() {
		return mappingDescriptor;	
	}
	
	public CyRow getRow() {
		return row;
	}
	
	public boolean isShowError() {
		return showError;
	}
	
	
}
