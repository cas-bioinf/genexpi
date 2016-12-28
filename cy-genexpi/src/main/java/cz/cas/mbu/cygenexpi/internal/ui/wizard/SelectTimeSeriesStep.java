package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
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

import cz.cas.mbu.cydataseries.DataSeriesEvent;
import cz.cas.mbu.cydataseries.DataSeriesListener;
import cz.cas.mbu.cydataseries.DataSeriesManager;
import cz.cas.mbu.cydataseries.DataSeriesMappingEvent;
import cz.cas.mbu.cydataseries.DataSeriesMappingListener;
import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.DataSeriesPublicTasks;
import cz.cas.mbu.cydataseries.MappingDescriptor;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.PredictionService;
import cz.cas.mbu.cygenexpi.internal.tasks.CheckConfigurationTask;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JSeparator;
import java.awt.Font;
import java.awt.event.ItemEvent;

import javax.swing.SwingConstants;
import javax.swing.JTextField;

public class SelectTimeSeriesStep extends JPanel implements WizardStep<GNWizardData>, DataSeriesMappingListener, DataSeriesListener {

	private GNWizardData data;
	
	private JComboBox<String> comboBoxTimeSeries;
	
	private CyServiceRegistrar registrar;

	private JButton btnMapTimeSeries;

	private JButton btnSmoothTimeSeries;

	private JComboBox<CyNetwork> networkComboBox;
	
	private boolean manipulatingNetworkModel = false;
	
	/**
	 * Create the panel.
	 */
	public SelectTimeSeriesStep() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
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
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblDescription = new JLabel("<html>First we need a time series for the expression data. The time series needs to be mapped to nodes in the network.\r\n</html>");
		add(lblDescription, "2, 2, 3, 1");
		
		JLabel lblSelectANetwork = new JLabel("Select a network:");
		add(lblSelectANetwork, "2, 4, right, default");
		
		networkComboBox = new JComboBox<>();
		add(networkComboBox, "4, 4, fill, default");
		networkComboBox.addItemListener(this::networkItemEvent);
		
		JLabel lblExpressionTimeSeries = new JLabel("<html>Select a column mapped to a time series:</html>");
		add(lblExpressionTimeSeries, "2, 6, right, default");
		
		comboBoxTimeSeries = new JComboBox<>();
		add(comboBoxTimeSeries, "4, 6, fill, default");
		
		JSeparator separator = new JSeparator();
		add(separator, "2, 10, 3, 1");
		
		JLabel lblifYouHave = new JLabel("<html>If you have no time series in your session, you need to import it:</html>");
		add(lblifYouHave, "2, 12, 3, 1");
		
		JButton btnImportTabular = new JButton("Import From Text File (.CSV,.TSV etc.)");
		add(btnImportTabular, "2, 14, 3, 1");
		btnImportTabular.addActionListener(evt -> importTimeSeries());
		
		JButton btnImportFromSoft = new JButton("Import From SOFT file");
		add(btnImportFromSoft, "2, 16, 3, 1");
		btnImportFromSoft.addActionListener(evt -> importFromSoftFile());
		
		JSeparator separator_1 = new JSeparator();
		add(separator_1, "2, 18, 3, 1");
		
		JLabel lblNewLabel = new JLabel("<html>\r\nIf you have imported the time series, but you do not see it in the selection below, you need to map it to nodes.\r\n</html>");
		add(lblNewLabel, "2, 20, 3, 1");
		
		btnMapTimeSeries = new JButton("Map Time Series to Nodes");
		add(btnMapTimeSeries, "2, 22, 3, 1");
		btnMapTimeSeries.addActionListener(evt -> mapTimeSeries());
		
		JSeparator separator_2 = new JSeparator();
		add(separator_2, "2, 24, 3, 1");
		
		JLabel lblInMostCases = new JLabel("<html>\r\nIn most cases you also want to smooth the time series, if you have not done that in another software.\r\n</html>");
		add(lblInMostCases, "2, 26, 3, 1");
		
		btnSmoothTimeSeries = new JButton("Smooth a Time Series");
		add(btnSmoothTimeSeries, "2, 28, 3, 1");
		btnSmoothTimeSeries.addActionListener(evt -> smoothTimeSeries());

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
	
	protected void networkItemEvent(ItemEvent e)
	{
		if(manipulatingNetworkModel)
		{
			return;
		}
		if(e.getStateChange() == ItemEvent.SELECTED)
		{
			if(networkComboBox.getSelectedIndex() >= 0)
			{
				CyNetwork newNetwork = networkComboBox.getItemAt(networkComboBox.getSelectedIndex());
				if(newNetwork != data.selectedNetwork)
				{
					data.selectedNetwork = newNetwork;
					refreshUI();					
				}
			}
		}
	}

	protected void refreshUI()
	{		
		manipulatingNetworkModel = true;
		Set<CyNetwork> networks = registrar.getService(CyNetworkManager.class).getNetworkSet();
		networkComboBox.setModel(new DefaultComboBoxModel<>(networks.toArray(new CyNetwork[networks.size()])));
		networkComboBox.setSelectedItem(data.selectedNetwork);		
		manipulatingNetworkModel = false;
		
		DataSeriesMappingManager mappingManager = registrar.getService(DataSeriesMappingManager.class );
		Map<String, TimeSeries> mappings = mappingManager.getAllMappings(data.selectedNetwork, CyNode.class, TimeSeries.class);
		
		CyTable nodeTable = data.selectedNetwork.getDefaultNodeTable();
		
		List<String> possibleSourceColumns = mappings.keySet().stream()
				.filter(col -> (nodeTable.getColumn(col) != null))
				.collect(Collectors.toList());
		
		comboBoxTimeSeries.setModel(new DefaultComboBoxModel<>(possibleSourceColumns.toArray( new String[ possibleSourceColumns.size()])));
		comboBoxTimeSeries.setSelectedItem(data.expressionMappingColumn); //This will not change the selection, if the column is not in the list, because the combo box is uneditable
		
		boolean anyTimeSeries = !registrar.getService(DataSeriesManager.class).getDataSeriesByType(TimeSeries.class).isEmpty();
		btnMapTimeSeries.setEnabled(anyTimeSeries);
		btnSmoothTimeSeries.setEnabled(anyTimeSeries);
	}

	
	@Override
	public void beforeStep(TaskMonitor taskMonitor)
	{	
		registrar.getService(DialogTaskManager.class).execute(new TaskIterator(new CheckConfigurationTask(registrar)));
		
		refreshUI();
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
		TaskIterator importTaskIterator = dsTasks.getImportDataSeriesTabularTask(TimeSeries.class);
		
		registrar.getService(DialogTaskManager.class).execute(importTaskIterator);
	}
	
	private void importFromSoftFile()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator importTaskIterator = dsTasks.getImportSoftFileTask(TimeSeries.class);
		
		registrar.getService(DialogTaskManager.class).execute(importTaskIterator);		
	}
	
	private void mapTimeSeries()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator mapTaskIterator = dsTasks.getMapDataSeriesTask();
		
		registrar.getService(DialogTaskManager.class).execute(mapTaskIterator);
		
	}
	
	private void smoothTimeSeries()
	{
		DataSeriesPublicTasks dsTasks = registrar.getService(DataSeriesPublicTasks.class);
		TaskIterator smoothTaskIterator = dsTasks.getInteractiveSmoothingTask();		
		registrar.getService(DialogTaskManager.class).execute(smoothTaskIterator);		
	}
	
	
	
	@Override
	public void handleEvent(DataSeriesMappingEvent evt) {
		refreshUI();
	}
	
	

	@Override
	public void handleEvent(DataSeriesEvent event) {
		refreshUI();
	}

	@Override
	public void wizardStarted() {
		registrar.registerService(this, DataSeriesMappingListener.class, new Properties());
		registrar.registerService(this, DataSeriesListener.class, new Properties());
	}

	@Override
	public void wizardClosed() {
		registrar.unregisterAllServices(this);
	}
	
}
