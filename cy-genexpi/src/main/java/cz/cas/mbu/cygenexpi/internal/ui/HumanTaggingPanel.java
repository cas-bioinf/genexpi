package cz.cas.mbu.cygenexpi.internal.ui;

import javax.swing.JPanel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYIntervalDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

import cz.cas.mbu.cydataseries.DataSeriesException;
import cz.cas.mbu.cydataseries.DataSeriesMappingManager;
import cz.cas.mbu.cydataseries.TimeSeries;
import cz.cas.mbu.cygenexpi.ProfileTags;
import cz.cas.mbu.cygenexpi.TaggingService;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;
import cz.cas.mbu.cygenexpi.internal.TaggingInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import com.jgoodies.forms.layout.FormLayout;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;
import java.awt.Font;
import java.awt.Paint;

public class HumanTaggingPanel extends JPanel {

	private final CyServiceRegistrar registrar;
		
	ChartPanel chartPanel;
	JFreeChart chart;
	XYPlot plot;
	
	DefaultXYDataset lineDataset;
	XYIntervalSeriesCollection ribbonDataset;
	XYLineAndShapeRenderer lineRenderer; 	
	DeviationRenderer ribbonRenderer;
	
	JTextField currentTagTextField;
	JLabel lblTitle;
	
	JComboBox<String> humanTagComboBox;
	
	TaggingInfo taggingInfo;
	/**
	 * Create the panel.
	 */
	public HumanTaggingPanel(CyServiceRegistrar serviceRegistrar) {
		setLayout(new BorderLayout(0, 0));
		
		lineDataset = new DefaultXYDataset();
		ribbonDataset = new XYIntervalSeriesCollection();
		
		lineRenderer = new XYLineAndShapeRenderer();
		lineRenderer.setBaseShapesVisible(false);
		lineRenderer.setBaseLinesVisible(true);
		
		ribbonRenderer = new DeviationRenderer();
		ribbonRenderer.setBaseShapesVisible(false);
		ribbonRenderer.setBaseLinesVisible(true);
		ribbonRenderer.setAutoPopulateSeriesFillPaint(true); //set to true to make the fill sequence stored in a DrawingSupplier used for ribbon fill (see below for the sequence)
		
		plot = new XYPlot(null, new NumberAxis("Time"), new NumberAxis(), null);
		plot.setDataset(0, ribbonDataset);
		plot.setRenderer(0, ribbonRenderer);
		plot.setDataset(1, lineDataset);
		plot.setRenderer(1, lineRenderer);
	
		
		chart = new JFreeChart(plot);		
		
		chartPanel = new ChartPanel(chart);
		add(chartPanel, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		
		JLabel lblCurrentTag = new JLabel("Current tag:");
		
		currentTagTextField = new JTextField();
		currentTagTextField.setEditable(false);
		currentTagTextField.setColumns(3);
		FormLayout fl_panel = new FormLayout(new ColumnSpec[] {
				ColumnSpec.decode("74px"),
				FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("108px:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormSpecs.UNRELATED_GAP_ROWSPEC,
				RowSpec.decode("20px"),});
		fl_panel.setColumnGroup(3,7);
		panel.setLayout(fl_panel);
		panel.add(lblCurrentTag, "1, 2, left, center");
		panel.add(currentTagTextField, "3, 2, fill, top");
		
		JLabel lblOverrideSuggestedTag = new JLabel("Change tag to:");
		panel.add(lblOverrideSuggestedTag, "5, 2, right, default");
		
		humanTagComboBox = new JComboBox<String>();
		panel.add(humanTagComboBox, "7, 2, fill, default");
		
		lblTitle = new JLabel("Title");
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 14));
		add(lblTitle, BorderLayout.NORTH);

		this.registrar = serviceRegistrar;
	}
	
	protected String getSeriesName(TimeSeries ts, int row, boolean withError)
	{
		String withErrorString = "";
		if(withError)
		{
			withErrorString = " + error margin";
		}
		return ts.getRowName(row) + withErrorString + " (ID " + ts.getRowID(row) + " in " + ts.getName() + ")";		
	}

	protected DrawingSupplier createDrawingSupplier()
	{
		//Our paint sequence (the default contains very light yellow, which is not legible
		Paint[] paintSequence = new Paint[] {
	            new Color(0xFF, 0x55, 0x55),
	            new Color(0x55, 0x55, 0xFF),
	            new Color(116,71,48),
	            new Color(0x55, 0xFF, 0x55),
	            new Color(0xFF, 0x55, 0xFF),
	            new Color(0x55, 0xFF, 0xFF),
	            Color.pink,
	            Color.gray,
	            ChartColor.DARK_RED,
	            ChartColor.DARK_BLUE,
	            ChartColor.DARK_GREEN,
	            ChartColor.DARK_YELLOW,
	            ChartColor.DARK_MAGENTA,
	            ChartColor.DARK_CYAN,
	            Color.darkGray,
	            ChartColor.LIGHT_RED,
	            ChartColor.LIGHT_BLUE,
	            ChartColor.LIGHT_GREEN,
	            ChartColor.LIGHT_YELLOW,
	            ChartColor.LIGHT_MAGENTA,
	            ChartColor.LIGHT_CYAN,
	            Color.lightGray,
	            ChartColor.VERY_DARK_RED,
	            ChartColor.VERY_DARK_BLUE,
	            ChartColor.VERY_DARK_GREEN,
	            ChartColor.VERY_DARK_YELLOW,
	            ChartColor.VERY_DARK_MAGENTA,
	            ChartColor.VERY_DARK_CYAN,
	            ChartColor.VERY_LIGHT_RED,
	            ChartColor.VERY_LIGHT_BLUE,
	            ChartColor.VERY_LIGHT_GREEN,
	            ChartColor.VERY_LIGHT_YELLOW,
	            ChartColor.VERY_LIGHT_MAGENTA,
	            ChartColor.VERY_LIGHT_CYAN
	        };		
		
		//Create fill paint sequence derived from the default paint sequence to have ribbons match the lines		
		Paint[] fillPaintSequence = Arrays.stream(paintSequence)
				.map(paint -> 
				{
					if(paint instanceof Color)
					{
						Color c = (Color)paint;
						float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
						return Color.getHSBColor(hsb[0], hsb[1] / 3, hsb[2]); //The same color, just less saturated
					}
					else
					{
						return paint;
					}
				})
				.toArray(Paint[]::new);

		//Replace the drawing supplier with a supplier with all defaults, but a different paint/fill paint sequences
		return new DefaultDrawingSupplier(paintSequence,
				fillPaintSequence, 
				DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE, 
				DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);

		
	}
	
	public void setData(CyRow row, TaggingMode mode, TaggingInfo info)
	{
		this.taggingInfo = info;
		DrawingSupplier drawingSupplier = createDrawingSupplier();
		
		lblTitle.setText(info.getSeriesProvider().getTaggingTitle(row));
		
		TaggingService taggingService = registrar.getService(TaggingService.class);
		String currentTag = taggingService.getProfileTag(row);
		if(currentTag.isEmpty())
		{
			currentTagTextField.setText(info.getNoTagCaption());
		}
		else
		{
			currentTagTextField.setText(currentTag);
		}
		
		Color bgColor = ProfileTags.getBackgroundColorForTag(currentTag);
		if(bgColor == null)
		{
			bgColor = Color.WHITE;
		}
		chart.setBackgroundPaint(bgColor);
		
		List<String> tagPossibilities = new ArrayList<>();
		tagPossibilities.add("-- Approve current --");		
		boolean containsNoTag = Arrays.asList(info.getPossibleTags()).contains("");
		if(containsNoTag)
		{
			tagPossibilities.add(info.getNoTagCaption());
		}
		
		Arrays.stream(info.getPossibleTags())
			.filter(tag -> !tag.equals(currentTag) && !tag.isEmpty())
			.forEach(tag -> tagPossibilities.add(tag));
		
		humanTagComboBox.setModel(new DefaultComboBoxModel<>(tagPossibilities.toArray(new String[tagPossibilities.size()])));
				
		
		//Remove all data
		while(lineDataset.getSeriesCount() > 0)
		{
			lineDataset.removeSeries(lineDataset.getSeriesKey(0));
		}
		ribbonDataset.removeAllSeries();
						
		
		info.getSeriesProvider().getSeriesForTagging(row).forEach(descriptorToProcess -> {
			if(descriptorToProcess.isShowError())
			{
				int tsRow = descriptorToProcess.getMappingDescriptor().getDataSeriesRow(descriptorToProcess.getRow());
				TimeSeries ts = descriptorToProcess.getMappingDescriptor().getDataSeries();
				if(tsRow >= 0)
				{	
					XYIntervalSeries seriesForchart = new XYIntervalSeries(getSeriesName(ts, tsRow, true /*with error*/));
					double[] rowData = ts.getRowDataArray(tsRow);
					for(int index = 0; index < ts.getIndexCount(); index++)
					{
						double time = ts.getIndexArray()[index];
						double center = rowData[index];
						double margin = info.getError().getErrorMargin(center);
						seriesForchart.add(new XYIntervalDataItem(time, time, time, center, Math.max(center - margin, 0), Math.max(center + margin, 0)), false);
					}
					int newIndex = ribbonDataset.getSeriesCount();
					ribbonDataset.addSeries(seriesForchart);		
					
					ribbonRenderer.setSeriesStroke(newIndex, drawingSupplier.getNextStroke());
					ribbonRenderer.setSeriesFillPaint(newIndex, drawingSupplier.getNextFillPaint());
				}								
			}
			else
			{
				int tsRow = descriptorToProcess.getMappingDescriptor().getDataSeriesRow(descriptorToProcess.getRow());
				TimeSeries ts = descriptorToProcess.getMappingDescriptor().getDataSeries();
				if(tsRow >= 0)
				{	
					double [][] seriesData = new double[][] { ts.getIndexArray(), ts.getRowDataArray(tsRow) };
					int newIndex = lineDataset.getSeriesCount();
					lineDataset.addSeries(getSeriesName(ts, tsRow, false /* without error*/), seriesData);							

					lineRenderer.setSeriesStroke(newIndex, drawingSupplier.getNextStroke());
					lineRenderer.setSeriesFillPaint(newIndex, drawingSupplier.getNextFillPaint());
				}							
				
			}
		});
							
	}
	
	/**
	 * 
	 * @return null if the current tag should be kept
	 */
	public String getSelectedTag()
	{
		if(humanTagComboBox.getSelectedIndex() == 0) //zeroth index is for the "Do not change" option
		{
			return null;
		}
		boolean containsNoTag = Arrays.asList(taggingInfo.getPossibleTags()).contains("");
		if(containsNoTag && humanTagComboBox.getSelectedIndex() == 1) //first index is reserved for empty tag (if possible).
		{
			return "";
		}

		return humanTagComboBox.getItemAt(humanTagComboBox.getSelectedIndex());
	}
}

