package cz.cas.mbu.cygenexpi;

import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;

public interface TaggingService {
	static final String PROFILE_TAG_COLUMN_NAME = "expression_profile_tag";
	static final String HUMAN_APPROVAL_TAG_COLUMN_NAME = "human_approval_tag";
	

	/**
	 * Sets the column and ensures it exists
	 * @param row
	 * @param tag
	 */
	void setProfileTag(CyRow row, String tag);
	String getProfileTag(CyRow row);
	
	void setHumanApprovalTag(CyRow row, String tag);
	String getHumanApprovalTag(CyRow row);
	
	List<CyRow> getNodeRowsPendingApproval(CyNetwork network);	
	List<CyRow> getEdgeRowsPendingApproval(CyNetwork network);
}
