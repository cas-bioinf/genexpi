package cz.cas.mbu.cygenexpi.internal;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.AbstractGUITunableHandler;

import cz.cas.mbu.cydataseries.internal.ui.SoftFileImportParametersPanel;
import cz.cas.mbu.cydataseries.internal.ui.TabularImportParametersPanel;
import cz.cas.mbu.cygenexpi.internal.ui.ConfigurationHelpPanel;

public class ConfigurationHelpGUIHandler extends AbstractGUITunableHandler {

	private final Logger userLogger = Logger.getLogger(CyUserLog.NAME);
	private final Logger logger = Logger.getLogger(ConfigurationHelpGUIHandler.class);
	
	private ConfigurationHelpPanel helpPanel;
		
	public ConfigurationHelpGUIHandler(Field field, Object instance, Tunable tunable) {
		super(field, instance, tunable);
		init();
	}

	public ConfigurationHelpGUIHandler(Method getter, Method setter, Object instance, Tunable tunable) {
		super(getter, setter, instance, tunable);
		init();
	}

	private void init() {
		try {
			ConfigurationHelp helpInfo = (ConfigurationHelp) getValue();
						
			helpPanel = new ConfigurationHelpPanel();
			helpPanel.setData(helpInfo);
				
				panel = helpPanel;
		} catch(Exception ex)
		{
			panel = new JPanel(new BorderLayout());
			panel.add(new JLabel("Error getting value: " + ex.getMessage()));
			
			userLogger.error("Could not create SOFT file import GUI", ex);
			logger.error("Could not create SOFT file import GUI", ex);
		}
	}
	
	@Override
	public void handle() {
		//This is a read-only GUI.
	}
}
