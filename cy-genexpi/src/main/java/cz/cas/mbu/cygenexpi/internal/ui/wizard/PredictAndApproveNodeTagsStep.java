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
import cz.cas.mbu.cygenexpi.internal.TaggingSeriesDescriptor;
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

public class PredictAndApproveNodeTagsStep extends JPanel implements WizardStep<GNWizardData> {
	private JTextField textFieldNumberOfNodes;
	private JTextField textFieldNoChangeNodes;
	private JTextField textFieldConstantSynthesis;

	private CyServiceRegistrar registrar;
	private GNWizardData data;
	
	private BatchTaggingPanel taggingPanel;
	/**
	 * Create the panel.
	 */
	public PredictAndApproveNodeTagsStep() {
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
		
		JLabel lblDescription = new JLabel("<html>We have marked some of the nodes profiles as having no significant change under the error bounds you gave in previous steps. Some profiles may also have been fit by the constant synthesis model.\r\n<br>\r\nYou may now review the tags that were assigned automatically in the results panel and change them if you find them incorrect.\r\n</html>");
		add(lblDescription, "2, 2, 3, 1");
		
		JLabel lblTotalNumberOf = new JLabel("Total number of nodes with expression profiles");
		add(lblTotalNumberOf, "2, 4, right, default");
		
		textFieldNumberOfNodes = new JTextField();
		textFieldNumberOfNodes.setEditable(false);
		add(textFieldNumberOfNodes, "4, 4, fill, default");
		textFieldNumberOfNodes.setColumns(10);
		
		JLabel lblNodesWithNo = new JLabel("Nodes with no change");
		add(lblNodesWithNo, "2, 6, right, default");
		
		textFieldNoChangeNodes = new JTextField();
		textFieldNoChangeNodes.setEditable(false);
		add(textFieldNoChangeNodes, "4, 6, fill, default");
		textFieldNoChangeNodes.setColumns(10);
		
		JLabel lblNodesWithConstant = new JLabel("Nodes with constant synthesis");
		add(lblNodesWithConstant, "2, 8, right, default");
		
		textFieldConstantSynthesis = new JTextField();
		textFieldConstantSynthesis.setEditable(false);
		add(textFieldConstantSynthesis, "4, 8, fill, default");
		textFieldConstantSynthesis.setColumns(10);

	}

	@Override
	public String getStepName() {
		return "Approve node tags";
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public ValidationState validate(StringBuilder messageBuilder) {
		int numPending = registrar.getService(TaggingService.class).getNodeRowsPendingApproval(data.selectedNetwork).size(); 
		if(numPending > 0)
		{
			messageBuilder.append("There are still " + numPending + " nodes pending approval, do you wish to continue anyway?\nYou can approve the nodes in the results panel.");
			return ValidationState.REQUEST_CONFIRMATION;
		}
		return ValidationState.OK;
	}
	
	@Override
	public void beforeStep(TaskMonitor taskMonitor) {		
		TaggingService taggingService = registrar.getService(TaggingService.class);
		CyTable nodeTable = data.selectedNetwork.getDefaultNodeTable();
		
		taggingService.clearAllProfileTags(nodeTable);
		taggingService.clearAllHumanApprovalTags(nodeTable);
		
		PredictionService predictionService = registrar.getService(PredictionService.class);		
		predictionService.markNoChangeGenes(data.selectedNetwork, data.expressionMappingColumn, data.errorDef);

		
		if(data.useConstantSynthesis)
		{
			try {
				predictionService.markConstantSynthesis(taskMonitor, data.selectedNetwork, data.expressionMappingColumn, data.errorDef, data.minFitQuality, true /*storeFitsInTimeSeries*/, GNWizardData.CONSTANT_SYNTHESIS_SERIES_NAME, GNWizardData.CONSTANT_SYNTHESIS_COLUMN_NAME,  true /*storeParametersInNodeTable*/, "csynth_" /* parametersPrefix*/);
			} catch (SuspectGPUResetByOSException ex)
			{
				UIUtils.handleSuspectedGPUResetInTask(ex);
			}
		}
		else
		{
			nodeTable.getAllRows().forEach(row -> {
				if(taggingService.getProfileTag(row).equals(ProfileTags.CONSTANT_SYNTHESIS))
				{
					taggingService.setProfileTag(row, ProfileTags.NO_TAG);
				}
			});
		}
		
		//count the results stats
		int numTotal = 0;
		int numNoChange = 0;
		int numConstant = 0;
		for(CyRow row : nodeTable.getAllRows())
		{
			Integer key = row.get(data.expressionMappingColumn, DataSeriesMappingManager.MAPPING_COLUMN_CLASS);
			if(key != null)
			{
				numTotal++;
				String tag = taggingService.getProfileTag(row);
				if(ProfileTags.NO_CHANGE.equals(tag))
				{
					numNoChange++;
				}
				else if(ProfileTags.CONSTANT_SYNTHESIS.equals(tag))
				{
					numConstant++;
				}
			}
		}
		//Put the counts in GUI
		textFieldNumberOfNodes.setText(Integer.toString(numTotal));
		textFieldNoChangeNodes.setText(Integer.toString(numNoChange));
		textFieldConstantSynthesis.setText(Integer.toString(numConstant));
		
		taggingPanel = UITagging.startNodeProfilesTagging(registrar, data.selectedNetwork, data.expressionMappingColumn, data.errorDef, GNWizardData.CONSTANT_SYNTHESIS_COLUMN_NAME, false, false);		
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
