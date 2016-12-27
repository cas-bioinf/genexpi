package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;

import javax.swing.JPanel;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.genexpi.compute.RegulationType;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;

public class EnterErrorCheckingStep extends JPanel implements WizardStep<GNWizardData> {
	private JTextField textFieldRelativeError;
	private JTextField textFieldAbsoluteError;
	private JTextField textFieldQuality;

	private JCheckBox chckbxUseConstantSynthesis;
	
	private GNWizardData data;
	private CyServiceRegistrar registrar;
	private JTextField textFieldMinimalError;
	private JComboBox<RegulationType> comboBoxRegulationType;
	
	/**
	 * Create the panel.
	 */
	public EnterErrorCheckingStep() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
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
		
		JLabel lblNewLabel = new JLabel("<html>Now we need to know how much noise do you expect in your data.  We will consider any value that differs from the measured value by less than max(absoluteError + measuredValue * relativeError, minimalError) as non-distinguishable from the measured value.\r\n</html>");
		add(lblNewLabel, "2, 2, 3, 1");
		
		JLabel lblExpressionTimeSeries = new JLabel("Relative error");
		add(lblExpressionTimeSeries, "2, 4, right, default");
		
		textFieldRelativeError = new JTextField();
		add(textFieldRelativeError, "4, 4, fill, default");
		textFieldRelativeError.setColumns(10);
		
		JLabel lblAbsoluteError = new JLabel("Absolute error");
		add(lblAbsoluteError, "2, 6, right, default");
		
		textFieldAbsoluteError = new JTextField();
		add(textFieldAbsoluteError, "4, 6, fill, default");
		textFieldAbsoluteError.setColumns(10);
		
		JLabel lblMinialError = new JLabel("Minimal error");
		lblMinialError.setHorizontalAlignment(SwingConstants.TRAILING);
		add(lblMinialError, "2, 8, right, default");
		
		textFieldMinimalError = new JTextField();
		add(textFieldMinimalError, "4, 8, fill, default");
		textFieldMinimalError.setColumns(10);
		
		JSeparator separator = new JSeparator();
		add(separator, "2, 10, 3, 1");
		
		JLabel lblMinimumQualityFor = new JLabel("Minimum quality for fit");
		add(lblMinimumQualityFor, "2, 12, right, default");
		
		textFieldQuality = new JTextField();
		add(textFieldQuality, "4, 12, fill, default");
		textFieldQuality.setColumns(10);
		
		JLabel lblFitQualityIs = new JLabel("<html>Fit quality is the fraction of points that need to lie within the error margin for a fit to be considered viable. It ranges from 0 to 1. Values over 0.9 are not reccommended as they may amplify noise. If unsure, keep somewhere around 0.8 - you will be able to judge the individual fits by yourself which works better.</html>");
		lblFitQualityIs.setFont(new Font("Tahoma", Font.ITALIC, 11));
		add(lblFitQualityIs, "4, 14");
		
		chckbxUseConstantSynthesis = new JCheckBox("Exclude genes fitted with constant synthesis");
		add(chckbxUseConstantSynthesis, "2, 16, 3, 1");
		
		JLabel lblifCheckedWe = new JLabel("<html>If checked, we will first try to fit the profiles by a simple model with consant synthesis and constant decay. If the genes can be fit well by this model, they can be fit also by any combination of regulators and thus genes found to be fit well by constant synthesis will be excluded from fitting by potential regulators.</html>");
		lblifCheckedWe.setFont(new Font("Tahoma", Font.ITALIC, 11));
		add(lblifCheckedWe, "4, 18");
		
		JLabel lblCheckForRegulations = new JLabel("Regulations to consider");
		add(lblCheckForRegulations, "2, 20, right, default");
		
		comboBoxRegulationType = new JComboBox<>(RegulationType.values());
		add(comboBoxRegulationType, "4, 20, fill, default");

	}

	@Override
	public String getStepName() {
		return "Enter error handling data";
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public ValidationState validate(StringBuilder messageBuilder) {
		try {
			Double.parseDouble(textFieldRelativeError.getText());
			Double.parseDouble(textFieldAbsoluteError.getText());
			Double.parseDouble(textFieldMinimalError.getText());
			Double.parseDouble(textFieldQuality.getText());			
		}
		catch(NumberFormatException ex)
		{
			messageBuilder.append("Incorrect number format");
			return ValidationState.INVALID;
		}
		return ValidationState.OK;
	}
	
	@Override
	public void beforeStep(TaskMonitor taskMonitor) {
		textFieldRelativeError.setText(Double.toString(data.errorDef.relativeError));
		textFieldAbsoluteError.setText(Double.toString(data.errorDef.absoluteError));
		textFieldMinimalError.setText(Double.toString(data.errorDef.minimalError));
		textFieldQuality.setText(Double.toString(data.minFitQuality));
		chckbxUseConstantSynthesis.setSelected(data.useConstantSynthesis);
		comboBoxRegulationType.setSelectedItem(data.regulationType);
	}

	@Override
	public void performStep(TaskMonitor taskMonitor) {		
		data.errorDef.relativeError = Double.parseDouble(textFieldRelativeError.getText());
		data.errorDef.absoluteError = Double.parseDouble(textFieldAbsoluteError.getText());
		data.errorDef.minimalError = Double.parseDouble(textFieldMinimalError.getText());
		data.minFitQuality = Double.parseDouble(textFieldQuality.getText());
		data.useConstantSynthesis = chckbxUseConstantSynthesis.isSelected();

		if(comboBoxRegulationType.getSelectedIndex() >= 0)
		{
			data.regulationType = comboBoxRegulationType.getItemAt(comboBoxRegulationType.getSelectedIndex());
		}
	}

	@Override
	public void setData(GNWizardData data, CyServiceRegistrar registrar) {
		this.data = data;
		this.registrar = registrar;
	}

	@Override
	public void wizardStarted() {
	}

	@Override
	public void wizardClosed() {
	}
	
	
	

}
