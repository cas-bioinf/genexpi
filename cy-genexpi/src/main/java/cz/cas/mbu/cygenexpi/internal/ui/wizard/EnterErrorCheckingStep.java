package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;

import javax.swing.JPanel;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableValidator.ValidationState;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.JCheckBox;

public class EnterErrorCheckingStep extends JPanel implements WizardStep<GNWizardData> {
	private JTextField textFieldRelativeError;
	private JTextField textFieldAbsoluteError;
	private JTextField textFieldQuality;

	private JCheckBox chckbxUseConstantSynthesis;
	
	private GNWizardData data;
	private CyServiceRegistrar registrar;
	
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
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblNewLabel = new JLabel("<html>Now we need to know how much noise do you expect in your data.  We will consider any value that differs from the measured value by less than (absoluteError + measuredValue * relativeError) as non-distinguishable from the measured value.\r\n</html>");
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
		
		JLabel lblMinimumQualityFor = new JLabel("Minimum quality for fit");
		add(lblMinimumQualityFor, "2, 8, right, default");
		
		textFieldQuality = new JTextField();
		add(textFieldQuality, "4, 8, fill, default");
		textFieldQuality.setColumns(10);
		
		JLabel lblFitQualityIs = new JLabel("<html>Fit quality is the fraction of points that need to lie within the error margin for a fit to be considered viable. It ranges from 0 to 1. Values over 0.9 are not reccommended as they may amplify noise. If unsure, keep somewhere around 0.8 - you will be able to judge the individual fits by yourself which works better.</html>");
		lblFitQualityIs.setFont(new Font("Tahoma", Font.ITALIC, 11));
		add(lblFitQualityIs, "4, 10");
		
		chckbxUseConstantSynthesis = new JCheckBox("Exclude genes fitted with constant synthesis");
		add(chckbxUseConstantSynthesis, "2, 12, 3, 1");
		
		JLabel lblifCheckedWe = new JLabel("<html>If checked, we will first try to fit the profiles by a simple model with consant synthesis and constant decay. If the genes can be fit well by this model, they can be fit also by any combination of regulators and thus genes found to be fit well by constant synthesis will be excluded from fitting by potential regulators.</html>");
		lblifCheckedWe.setFont(new Font("Tahoma", Font.ITALIC, 11));
		add(lblifCheckedWe, "4, 14");

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
		textFieldQuality.setText(Double.toString(data.minFitQuality));
		chckbxUseConstantSynthesis.setSelected(data.useConstantSynthesis);
	}

	@Override
	public void performStep(TaskMonitor taskMonitor) {		
		data.errorDef.relativeError = Double.parseDouble(textFieldRelativeError.getText());
		data.errorDef.absoluteError = Double.parseDouble(textFieldAbsoluteError.getText());
		data.minFitQuality = Double.parseDouble(textFieldQuality.getText());
		data.useConstantSynthesis = chckbxUseConstantSynthesis.isSelected();
	}

	@Override
	public void setData(GNWizardData data, CyServiceRegistrar registrar) {
		this.data = data;
		this.registrar = registrar;
	}
	
	
	

}
