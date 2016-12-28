package cz.cas.mbu.cygenexpi.internal.ui;

import javax.swing.JPanel;
import javax.swing.JLabel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;

public class OpenCLTroubleshootingPanel extends JPanel {

	private JLabel lblSpecificText;

	/**
	 * Create the panel.
	 */
	public OpenCLTroubleshootingPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				ColumnSpec.decode("197px"),
				ColumnSpec.decode("56px"),},
			new RowSpec[] {
				FormSpecs.LINE_GAP_ROWSPEC,
				RowSpec.decode("14px"),
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		lblSpecificText = new JLabel("New label");
		add(lblSpecificText, "1, 2");
		
		JLabel lblHelpfulText = new JLabel("Helpful text");
		add(lblHelpfulText, "1, 4, left, top");

	}

	public void setSpecificMessage(String text)
	{
		lblSpecificText.setText(text);
	}
}
