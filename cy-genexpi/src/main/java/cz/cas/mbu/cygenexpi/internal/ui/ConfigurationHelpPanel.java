package cz.cas.mbu.cygenexpi.internal.ui;

import javax.swing.JPanel;
import javax.swing.JLabel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.cygenexpi.internal.ConfigurationHelp;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JButton;
import javax.swing.JSeparator;
import java.awt.Color;
import java.awt.Font;

public class ConfigurationHelpPanel extends JPanel {

	private static final String AMD_DRIVERS_LINK = "http://support.amd.com/en-us/kb-articles/Pages/OpenCL2-Driver.aspx";
	private static final String INTEL_DRIVERS_LINK = "https://software.intel.com/en-us/articles/opencl-drivers#latest_CPU_runtime";
	private JLabel lblSpecificText;

	/**
	 * Create the panel.
	 */
	public ConfigurationHelpPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormSpecs.LINE_GAP_ROWSPEC,
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
		
		lblSpecificText = new JLabel("Specific text");
		lblSpecificText.setFont(lblSpecificText.getFont().deriveFont(lblSpecificText.getFont().getStyle() | Font.BOLD, lblSpecificText.getFont().getSize() + 3f));
		lblSpecificText.setForeground(new Color(178, 34, 34));
		add(lblSpecificText, "1, 2");
		
		JLabel lblHelpfulText = new JLabel("<html>CyGenexpi requires OpenCL device to work. If your device is not available for selection, you need to install drivers:</html>");
		add(lblHelpfulText, "1, 4, left, top");
		
		JButton btnDriversForIntel = new JButton("Drivers for Intel processors (Web)");
		add(btnDriversForIntel, "1, 6");
		btnDriversForIntel.addActionListener(evt -> LinkSupport.openLink(INTEL_DRIVERS_LINK));
		
		JButton btnDriversForAmd = new JButton("Drivers for AMD processors (Web)");
		add(btnDriversForAmd, "1, 8");
		btnDriversForAmd.addActionListener(evt -> LinkSupport.openLink(AMD_DRIVERS_LINK));
		
		JLabel lblCytoscapeRestartMay = new JLabel("<html>Cytoscape restart may be required to show any freshly installed devices.</html>");
		add(lblCytoscapeRestartMay, "1, 10");

	}

	public void setData(ConfigurationHelp help)
	{
		String mainMessageText = "";
		switch(help.getMainMessage())
		{
			case None:
				mainMessageText = "";
				break;
			case NoDevices:
				mainMessageText = "No OpenCL devices are available";
				break;
			case DeviceTestFailed:
				mainMessageText = "Device test failed";
				break;
			case InitError:
				mainMessageText = "Error in OpenCL initialization - try (re)installing OpenCL drivers";
				break;
			default:
				mainMessageText = "Unexpected problem";					
		}
		
		if(help.getDetails().isEmpty())
		{
			lblSpecificText.setText(mainMessageText);
		}
		else if(mainMessageText.isEmpty())
		{
			String completeText = "<html>" + help.getDetails().replace("\n", "<br>") +  "</html>";
			lblSpecificText.setText(completeText);
		}
		else
		{
			String completeText = "<html>" + mainMessageText + ":<br>" + help.getDetails().replace("\n", "<br>") +  "</html>";
			lblSpecificText.setText(completeText);
		}
		
		revalidate();
		repaint();
	}
}
