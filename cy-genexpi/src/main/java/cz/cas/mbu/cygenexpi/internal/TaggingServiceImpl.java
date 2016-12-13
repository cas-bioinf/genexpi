package cz.cas.mbu.cygenexpi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;

public class TaggingServiceImpl implements TaggingService {

	protected void setTag(CyRow row, String tag, String columnName)
	{
		CyTable nodeTable = row.getTable();
		if(nodeTable.getColumn(columnName) == null)
		{
			nodeTable.createColumn(columnName, String.class, false);
		}
				
		row.set(columnName, tag);					
	}
	
	protected String getTag(CyRow row, String columnName)
	{
		String columnGet = row.get(columnName, String.class);
		if(columnGet == null)
		{
			return "";
		}
		else 
		{
			return columnGet;
		}		
	}
	
	@Override
	public void setProfileTag(CyRow row, String tag) {
		setTag(row, tag, PROFILE_TAG_COLUMN_NAME);				
	}

	@Override
	public String getProfileTag(CyRow row) {
		return getTag(row, PROFILE_TAG_COLUMN_NAME);
	}

	@Override
	public void setHumanApprovalTag(CyRow row, String tag) {
		setTag(row, tag, HUMAN_APPROVAL_TAG_COLUMN_NAME);		
	}

	@Override
	public String getHumanApprovalTag(CyRow row) {
		return getTag(row, HUMAN_APPROVAL_TAG_COLUMN_NAME);
	}

	@Override
	public List<CyRow> getNodeRowsPendingApproval(CyNetwork network) {
		return network.getDefaultNodeTable().getAllRows().stream()
				.filter(row -> getHumanApprovalTag(row).isEmpty())
				.collect(Collectors.toList());
	}
	
	@Override
	public List<CyRow> getEdgeRowsPendingApproval(CyNetwork network) {
		return network.getDefaultEdgeTable().getAllRows().stream()
				.filter(row -> getHumanApprovalTag(row).isEmpty())
				.collect(Collectors.toList());
	}

	@Override
	public void clearAllProfileTags(CyTable table) {
		table.getAllRows().forEach(row -> setProfileTag(row, ProfileTags.NO_TAG));		
	}

	@Override
	public void clearAllHumanApprovalTags(CyTable table) {
		table.getAllRows().forEach(row -> setHumanApprovalTag(row, HumanApprovalTags.NO_TAG));		
	}
	

}
