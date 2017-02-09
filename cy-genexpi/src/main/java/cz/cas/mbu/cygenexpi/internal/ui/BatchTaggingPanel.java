package cz.cas.mbu.cygenexpi.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.service.util.CyServiceRegistrar;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.cydataseries.DataSeriesException;
import cz.cas.mbu.cygenexpi.HumanApprovalTags;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.TaggingInfo;

public class BatchTaggingPanel extends JPanel implements CytoPanelComponent2 {

	public static final String PANEL_IDENTIFIER = "cz.cas.mbu.genexpi.batchTagging";
	
	JComboBox<DisplayFormat> displayGridComboBox;
	private JPanel taggingSetPanel;
	
	private final CyServiceRegistrar registrar;
	
	private TaggingInfo taggingInfo;
	
	private List<CyRow> currentlyShownRows;
	private List<HumanTaggingPanel> currentlyShownPanels;
	
	private TaggingMode mode;
	
	private String caption;
	
	private List<String> legendTexts;
	private List<Color> legendColors;
	
	/**
	 * Create the panel.
	 */
	public BatchTaggingPanel(CyServiceRegistrar registrar, TaggingInfo info, TaggingMode mode, String caption) {
		setMinimumSize(new Dimension(450, 600));
		setLayout(new BorderLayout(0, 0));
		
		taggingSetPanel = new JPanel();
		add(taggingSetPanel, BorderLayout.CENTER);
		
		JPanel controlPanel = new JPanel();
		add(controlPanel, BorderLayout.SOUTH);
		controlPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				ColumnSpec.decode("7dlu:grow"),
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblDisplay = new JLabel("Display:");
		controlPanel.add(lblDisplay, "2, 2, right, default");
		
		displayGridComboBox = new JComboBox<>(new DefaultComboBoxModel<DisplayFormat>(new DisplayFormat[] {
				new DisplayFormat(1, 1),
				new DisplayFormat(1, 2),
				new DisplayFormat(1, 5),
				new DisplayFormat(2, 2),				
				new DisplayFormat(3, 2),				
				new DisplayFormat(5, 1),				
				}));
		displayGridComboBox.setSelectedIndex(1);//TODO - use RememberValueService
		displayGridComboBox.addItemListener(evt -> {
			if(evt.getStateChange() == ItemEvent.SELECTED)
			{
				updateDisplayGrid();
			}
		});
		controlPanel.add(displayGridComboBox, "4, 2, fill, default");
		
		JSeparator separator = new JSeparator();
		controlPanel.add(separator, "1, 1, 8, 1");
		
		JButton btnApproveDisplayedTags = new JButton("Approve Tags & Continue");
		btnApproveDisplayedTags.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnApproveDisplayedTags.addActionListener(evt -> approveAllSelectedTags());
		controlPanel.add(btnApproveDisplayedTags, "6, 2, 3, 1");
		
		JButton btnRefresh = new JButton("Refresh");
		controlPanel.add(btnRefresh, "6, 4");
		btnRefresh.addActionListener(evt -> updateDisplayGrid());
		
		JButton btnClose = new JButton("Close");
		controlPanel.add(btnClose, "8, 4");
		btnClose.addActionListener(evt -> closePanel());

		this.registrar = registrar;
		this.mode = mode;
		this.taggingInfo = info;
		this.caption = caption;
		
		this.legendTexts = new ArrayList<>();
		this.legendColors = new ArrayList<>();
		
		for(String tag: info.getPossibleTags())
		{
			Color color = ProfileTags.getBackgroundColorForTag(tag);
			if(color != null)
			{
				int existingIndex = legendColors.indexOf(color);
				if(existingIndex >= 0)
				{
					legendTexts.set(existingIndex, legendTexts.get(existingIndex) + ", " + tag);
				}
				else
				{
					legendColors.add(color);
					legendTexts.add(tag);
				}
			}
			
		}
		
		if(!java.beans.Beans.isDesignTime())
		{
			updateDisplayGrid();
		}
	}

	
	
	@Override
	public Component getComponent() {		
		return this;
	}



	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}



	@Override
	public String getTitle() {
		return caption;
	}



	@Override
	public Icon getIcon() {
		return null;
	}



	@Override
	public String getIdentifier() {
		return PANEL_IDENTIFIER;
	}



	private void updateDisplayGrid()
	{
		DisplayFormat fmt = displayGridComboBox.getItemAt(displayGridComboBox.getSelectedIndex());
		taggingSetPanel.removeAll();

		int maxDisplayed = fmt.getWidth() * fmt.getHeight();
		CyNetwork network = taggingInfo.getSeriesProvider().getNetwork();
		List<CyRow> candidateRows;
		if(mode == TaggingMode.NODE)
		{
			candidateRows = registrar.getService(TaggingService.class).getNodeRowsPendingApproval(network);
		}
		else if(mode == TaggingMode.EDGE)
		{
			candidateRows = registrar.getService(TaggingService.class).getEdgeRowsPendingApproval(network);			
		}
		else
		{
			throw new DataSeriesException("Unrecognized mode");
		}
		
		//Filter rows with no series and limit max rows to the max displayed
		currentlyShownRows = candidateRows.stream()
				.filter(row -> taggingInfo.getSeriesProvider().isRelevant(row))
				.limit(maxDisplayed)
				.collect(Collectors.toList());		
		
		if(currentlyShownRows.isEmpty())
		{
			taggingSetPanel.setLayout(new BorderLayout());
			JLabel noMoreLabel = new JLabel("No more profiles to tag.");
			noMoreLabel.setFont(noMoreLabel.getFont().deriveFont(Font.BOLD, 18));
			noMoreLabel.setHorizontalAlignment(JLabel.CENTER);
			taggingSetPanel.add(noMoreLabel,BorderLayout.CENTER);
			taggingSetPanel.revalidate();
			taggingSetPanel.repaint();
			return;
		}

		currentlyShownPanels = currentlyShownRows.stream()
				.map(row -> {
					HumanTaggingPanel panel = new HumanTaggingPanel(registrar);
					panel.setData(row, mode, taggingInfo);
					return panel;
				})
				.collect(Collectors.toList());
		
		ColumnSpec[] layoutColumns = new ColumnSpec[fmt.width * 2 - 1];
		RowSpec[] layoutRows = new RowSpec[(fmt.height + 1) * 2 - 1]; //1 row and 1 gap extra for the legend		
		
		for(int i = 0; i < fmt.width; i++)
		{
			if(i != 0)
			{
				layoutColumns[i * 2 - 1] = FormSpecs.RELATED_GAP_COLSPEC;
			}
			layoutColumns[i * 2] = FormSpecs.DEFAULT_COLSPEC;
		}
		
		for(int i = 0; i < fmt.height + 1; i++) //1 extra row for the legend
		{
			if(i != 0)
			{
				layoutRows[i * 2 - 1] = FormSpecs.RELATED_GAP_ROWSPEC;
			}
			layoutRows[i * 2] = FormSpecs.DEFAULT_ROWSPEC;
			
		}
		
		taggingSetPanel.setLayout(new FormLayout(layoutColumns, layoutRows));
		
		CellConstraints cc = new CellConstraints();
		for(int x = 0; x < fmt.getWidth(); x++)
		{
			for(int y = 0; y < fmt.getHeight(); y++)
			{
				int index = (y * fmt.getWidth()) + x;
				JPanel panelToAdd;
				if(index >= currentlyShownRows.size())
				{
					panelToAdd = new JPanel();
				}
				else
				{
					panelToAdd = currentlyShownPanels.get(index);
				}
				
				int layoutX = x * 2 + 1;
				int layoutY = y * 2 + 1;				
				taggingSetPanel.add(panelToAdd, cc.xy(layoutX, layoutY));
			}
		}
		
		//Create the legend
		RowSpec[] legendRowSpec = new RowSpec[] { FormSpecs.DEFAULT_ROWSPEC };
		ColumnSpec[] legendColSpec = new ColumnSpec[legendTexts.size() * 2 + 1];
		legendColSpec[0] = FormSpecs.DEFAULT_COLSPEC;
		for(int legendId = 0; legendId < legendTexts.size(); legendId++)
		{
			legendColSpec[legendId * 2 + 1] = FormSpecs.RELATED_GAP_COLSPEC;
			legendColSpec[legendId * 2 + 2] = FormSpecs.DEFAULT_COLSPEC;
		}
		
		JPanel legendPanel = new JPanel(new FormLayout(legendColSpec, legendRowSpec));
		JLabel legendLabel = new JLabel("Legend:");
		legendPanel.add(legendLabel, cc.xy(1, 1));
		for(int legendId = 0; legendId < legendTexts.size(); legendId++)
		{
			JPanel singleLabelPanel = new JPanel();
			JLabel singleLabel = new JLabel(legendTexts.get(legendId));
			singleLabelPanel.add(singleLabel);
			singleLabelPanel.setBackground(legendColors.get(legendId));
			legendPanel.add(singleLabelPanel, cc.xy(legendId * 2 + 3, 1));
		}		
		
		taggingSetPanel.add(new JSeparator(JSeparator.HORIZONTAL), cc.xywh(1, fmt.height * 2, fmt.width *2 - 1, 1));
		taggingSetPanel.add(legendPanel, cc.xywh(1, fmt.height * 2 + 1 , fmt.width * 2 - 1, 1));
		
		taggingSetPanel.revalidate();
		taggingSetPanel.repaint();
	}
	
	private void approveAllSelectedTags()
	{
		TaggingService taggingService = registrar.getService(TaggingService.class);
		for(int i = 0; i < currentlyShownRows.size(); i++)
		{
			CyRow row = currentlyShownRows.get(i);
			HumanTaggingPanel panel = currentlyShownPanels.get(i);
			
						
			if( taggingService.getHumanApprovalTag(row).equals(HumanApprovalTags.EDITED) 
					|| panel.getSelectedTag() != null )
			{
				taggingService.setHumanApprovalTag(row, HumanApprovalTags.EDITED);
			}
			else
			{
				taggingService.setHumanApprovalTag(row, HumanApprovalTags.APPROVED);				
			}
			
			if(panel.getSelectedTag() != null)
			{
				taggingService.setProfileTag(row, panel.getSelectedTag());
			}
		}
		
		updateDisplayGrid();
	}
	
	public void closePanel()
	{
		registrar.unregisterAllServices(this);
	}
	
	private static class DisplayFormat
	{
		private final int width;
		private final int height;
		
		public DisplayFormat(int width, int height) {
			super();
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		@Override
		public String toString() {
			return width + "x" + height;
		}
		
		
	}

}
