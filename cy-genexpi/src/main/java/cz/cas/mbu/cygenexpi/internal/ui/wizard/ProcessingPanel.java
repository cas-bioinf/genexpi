package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Font;

public class ProcessingPanel extends JPanel {

	/**
	 * Create the panel.
	 */
	public ProcessingPanel() {
		
		JLabel lblProcessing = new JLabel("Processing...");
		lblProcessing.setFont(lblProcessing.getFont().deriveFont(lblProcessing.getFont().getStyle() | Font.BOLD, lblProcessing.getFont().getSize() + 5f));
		add(lblProcessing);

	}

}
