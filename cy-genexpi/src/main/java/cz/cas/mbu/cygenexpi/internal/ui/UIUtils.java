package cz.cas.mbu.cygenexpi.internal.ui;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.service.util.CyServiceRegistrar;

import cz.cas.mbu.cygenexpi.ConfigurationService;
import cz.cas.mbu.genexpi.compute.SuspectGPUResetByOSException;

public class UIUtils {
	public static void ensurePanelVisible(CyServiceRegistrar registrar, CytoPanelComponent panel)
	{
		CytoPanel cytoPanel = registrar.getService(CySwingApplication.class).getCytoPanel(panel.getCytoPanelName());
		if(cytoPanel.getState() == CytoPanelState.HIDE)
		{
			cytoPanel.setState(CytoPanelState.DOCK);
		}
		
		SwingUtilities.invokeLater(() -> {
			int index = cytoPanel.indexOfComponent(panel.getComponent());
			if(index >= 0)
			{
				cytoPanel.setSelectedIndex(index);;
			}
			
		});
		
	}
	
	public static void handleSuspectedGPUResetInTask(CyServiceRegistrar registrar, SuspectGPUResetByOSException ex)
	{
		if (!registrar.getService(ConfigurationService.class).isPreventFullOccupation())
		{
			throw new SuspectGPUResetByOSException(ex.getMessage() + "\nSee Edit -> Preferences -> OpenCL Settings.", ex);
		}
		else
		{
			throw ex;
		}
	}
}
