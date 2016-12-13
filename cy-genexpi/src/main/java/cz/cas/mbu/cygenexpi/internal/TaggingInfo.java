package cz.cas.mbu.cygenexpi.internal;

import java.util.List;

import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;

public class TaggingInfo {
	private final String[] possibleTags;
	private final String noTagCaption;
	private final TaggingSeriesProvider seriesProvider;
	private final ErrorDef error;

	


	public TaggingInfo(String[] possibleTags, String noTagCaption, TaggingSeriesProvider seriesProvider,
			ErrorDef error) {
		super();
		this.possibleTags = possibleTags;
		this.noTagCaption = noTagCaption;
		this.seriesProvider = seriesProvider;
		this.error = error;
	}

	public String[] getPossibleTags() {
		return possibleTags;
	}

	public TaggingSeriesProvider getSeriesProvider() {
		return seriesProvider;
	}

	public ErrorDef getError() {
		return error;
	}

	public String getNoTagCaption() {
		return noTagCaption;
	}	
	
}
