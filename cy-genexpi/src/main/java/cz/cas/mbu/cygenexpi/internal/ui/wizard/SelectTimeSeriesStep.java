package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.swing.DialogTaskManager;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.cydataseries.DataSeriesListener;
import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.DataSeriesPublicTasks;
import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.PredictionService;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JSeparator;

public class SelectTimeSeriesStep extends JPanel implements WizardStep<GNWizardData> {

	private GNWizardData data;
	
	private JComboBox<String> comboBoxTimeSeries;
	
	private CyServiceRegistrar registrar;
	
	/**
	 * Create the panel.
	 */
	public SelectTimeSeriesStep() {
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
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblDescription = new JLabel("<html>First we need a time series for the expression data. The time series needs to be mapped to nodes in the network.\r\n</html>");
		add(lblDescription, "2, 2, 3, 1");
		
		JLabel lblExpressionTimeSeries = new JLabel("<html>Select a column mapped to a time series</html>");
		add(lblExpressionTimeSeries, "2, 4, right, default");
		
		comboBoxTimeSeries = new JComboBox<>();
		add(comboBoxTimeSeries, "4, 4, fill, default");
		
		JSeparator separator = new JSeparator();
		add(separator, "2, 5, 3, 1");
		
		JLabel lblNewLabel = new JLabel("<html>\r\nIf you have imported the time series, but you do not see it in the selection below, you need to map it to nodes.<br>\r\nIn most cases you also want to smooth the time series, if you have not done that in another software.\r\n</html>");
		add(lblNewLabel, "2, 6, 3, 1");
		
		JButton btnImportATime = new JButton("Import a Time Series");
		add(btnImportATime, "4, 8");
		btnImportATime.addActionListener(evt -> importTimeSeries());
		
		JButton btnSmoothATime = new JButton("Smooth a Time Series");
		add(btnSmoothATime, "4, 10");
		btnSmoothATime.addActionListener(evt -> smoothTimeSeries());
		
		JButton btnMapTimeSeries = new JButton("Map Time Series to Nodes");
		add(btnMapTimeSeries, "4, 12");
		btnMapTimeSeries.addActionListener(evt -> mapTimeSeries());

	}

	@Override
	public String getStepName() {
		return "Select expression time series";
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public ValidationState validate(StringBuilder messageBuilder) {
		String selectedMappingColumn = getSelectedMappingColumn();
		if(selectedMappingColumn == null)
		{
			messageBuilder.append("You have to select a time series with expression data.\nIf there is none, you need to import it and map it to a column in the node table.");
			return ValidationState.INVALID;
		}
		
		TimeSeries expressionSeries = registrar.getService(DataSeriesMappingManager.class).getMappedDataSeries(data.selectedNetwork, CyNode.class, selectedMappingColumn, TimeSeries.class);
		ValidationState predictionValidation = registrar.getService(PredictionService.class).validateTimeSeriesForPrediction(expressionSeries, messageBuilder);
		if(predictionValidation != ValidationState.OK)
		{
			return predictionValidation;
		}
		return ValidationState.OK;
	}

	protected void refreshSeriesComboBox()
	{
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class );
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(data.selectedNetwork, CyNode.class, TimeSeries.class);
		
		CyTable nodeTable = data.selectedNetwork.getDefaultNodeTable();
		
		List<String> possibleSourceColumns = mappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());
		
		comboBoxTimeSeries.setModel(new DefaultComboBoxModel<>(possibleSourceColumns.toArray( new String[ possibleSourceColumns.size()])));
		comboBoxTimeSeries.setSelectedItem(data.expressionMappingColumn); //This will not change the selection, if the column is not in the list, because the combo box is uneditable
		
	}

	
	@Override
	public void beforeStep(TaskMonitor taskMonitor)
	{
		refreshSeriesComboBox();
	}
	
	@Override
	public void performStep(TaskMonitor taskMonitor) {
		data.expressionMappingColumn = getSelectedMappingColumn();
	}

	private String getSelectedMappingColumn() {
		if(comboBoxTimeSeries.getSelectedIndex() < 0)
		{
			return null;
		}
		else 
		{
			return comboBoxTimeSeries.getItemAt(comboBoxTimeSeries.getSelectedIndex());
		}
	}

	@Override
	public void setData(GNWizardData data, CyServiceRegistrar registrar) {
		this.data = data;
		this.registrar = registrar;
	}	
	
	private void importTimeSeries()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator importTaskIterator = dsTasks.getImportDataSeriesTask(TimeSeries.class);
		
		//End with a refresh of the GUI
		importTaskIterator.append(new RefreshGUITask());
		registrar.getService(DialogTaskManager.class).execute(importTaskIterator);
	}
	
	private void mapTimeSeries()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator mapTaskIterator = dsTasks.getMapDataSeriesTask();
		
		//End with a refresh of the GUI
		mapTaskIterator.append(new RefreshGUITask());
		registrar.getService(DialogTaskManager.class).execute(mapTaskIterator);
		
	}
	
	private void smoothTimeSeries()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator smoothTaskIterator = dsTasks.getInteractiveSmoothingTask();		
		registrar.getService(DialogTaskManager.class).execute(smoothTaskIterator);		
	}

	private final class RefreshGUITask extends AbstractTask {
		@Override
		public void run(TaskMonitor taskMonitor) throws Exception {
			SwingUtilities.invokeLater(() -> refreshSeriesComboBox());				
		}
	}
	
}
