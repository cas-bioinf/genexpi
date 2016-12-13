package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;

import javax.swing.JPanel;

import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.tasks.MarkNoChangeGenesTask;
import cz.cas.mbu.cygenexpi.internal.ui.BatchTaggingPanel;
import cz.cas.mbu.cygenexpi.internal.ui.UITagging;
import cz.cas.mbu.cygenexpi.internal.ui.UIUtils;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JTextField;

public class PredictAndApproveEdgeTagsStep extends JPanel implements WizardStep<GNWizardData> {
	private JTextField textFieldNumberOfProfiles;
	private JTextField textFieldGoodFit;
	private JTextField textFieldNoFit;

	private GNWizardData data;
	private CyServiceRegistrar registrar;
	
	private BatchTaggingPanel taggingPanel;
	
	/**
	 * Create the panel.
	 */
	public PredictAndApproveEdgeTagsStep() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblDescription = new JLabel("<html>The profiles for all relevant gene-regulator pairs have been fitted and automatically tagged as either good fit or no fit. \r\n<br>\r\nYou may now review the tags that were assigned automatically in the results panel and change them if you find them incorrect.\r\n</html>");
		add(lblDescription, "2, 2, 3, 1");
		
		JLabel lblTotalNumberOf = new JLabel("Total number of gene-regulator pairs tested");
		add(lblTotalNumberOf, "2, 4, right, default");
		
		textFieldNumberOfProfiles = new JTextField();
		add(textFieldNumberOfProfiles, "4, 4, fill, default");
		textFieldNumberOfProfiles.setColumns(10);
		
		JLabel lblNodesWithNo = new JLabel("Profiles with good fit");
		add(lblNodesWithNo, "2, 6, right, default");
		
		textFieldGoodFit = new JTextField();
		add(textFieldGoodFit, "4, 6, fill, default");
		textFieldGoodFit.setColumns(10);
		
		JLabel lblNodesWithConstant = new JLabel("Profiles without fit");
		add(lblNodesWithConstant, "2, 8, right, default");
		
		textFieldNoFit = new JTextField();
		add(textFieldNoFit, "4, 8, fill, default");
		textFieldNoFit.setColumns(10);

	}

	@Override
	public String getStepName() {
		return "Approve edge tags";
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public ValidationState validate(StringBuilder messageBuilder) {
		int numPending = registrar.getService(TaggingService.class).getEdgeRowsPendingApproval(data.selectedNetwork).size(); 
		if(numPending > 0)
		{
			messageBuilder.append("There are still " + numPending + " profiles pending approval, do you wish to continue anyway?\nYou can approve the nodes in the results panel.");
			return ValidationState.REQUEST_CONFIRMATION;
		}
		return ValidationState.OK;
	}

	@Override
	public void beforeStep(TaskMonitor taskMonitor) {
		PredictionService predictionService = registrar.getService(PredictionService.class);
		try {
			predictionService.predictSingleRegulations(taskMonitor, data.selectedNetwork, data.expressionMappingColumn, GNWizardData.PREDICTION_SERIES_NAME, GNWizardData.PREDICTION_COLUMN_NAME, true, "Prediction_", false, data.errorDef, data.minFitQuality);
		} catch (SuspectGPUResetByOSException ex)
		{
			UIUtils.handleSuspectedGPUResetInTask(ex);
		}
		
		//count the results stats
		TaggingService taggingService = registrar.getService(TaggingService.class);
		CyTable edgeTable = data.selectedNetwork.getDefaultEdgeTable();
		int numTotal = 0;
		int numGoodFit = 0;
		int numNoFit = 0;
		for(CyRow row : edgeTable.getAllRows())
		{
			Integer key = row.get(GNWizardData.PREDICTION_COLUMN_NAME, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
			if(key != null)
			{
				numTotal++;
				String tag = taggingService.getProfileTag(row);
				if(ProfileTags.GOOD_FIT.equals(tag))
				{
					numGoodFit++;
				}
				else if(ProfileTags.NO_FIT.equals(tag))
				{
					numNoFit++;
				}
			}
		}
		//Put the counts in GUI
		textFieldNumberOfProfiles.setText(Integer.toString(numTotal));
		textFieldNoFit.setText(Integer.toString(numNoFit));
		textFieldGoodFit.setText(Integer.toString(numGoodFit));
		
		taggingPanel = UITagging.startPredictedEdgeProfilesTagging(registrar, data.selectedNetwork, data.expressionMappingColumn, data.errorDef, GNWizardData.PREDICTION_COLUMN_NAME, false, false);		
	}
	
	@Override
	public void performStep(TaskMonitor taskMonitor) {
		taggingPanel.closePanel();
	}

	@Override
	public void setData(GNWizardData data, CyServiceRegistrar registrar) {
		this.data = data;
		this.registrar = registrar;
	}
	
	
	
	

}
