package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.awt.Component;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.swing.DialogTaskManager;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import cz.cas.mbu.cygenexpi.RememberValueService;

import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import java.awt.Font;

public class WizardPanel<DATA> extends JPanel implements CytoPanelComponent {


	private List<WizardStep<DATA>> steps;
	private int shownStepIndex;
	
	private String title;
	
	private JLabel lblTitle; 
	private JButton btnNext;
	private JButton btnPrevious;
	
	CyServiceRegistrar registrar;
	
	private DATA data; 
	private JButton btnCancel;
	
	private JPanel processingPanel;
	private final CellConstraints stepComponentConstraints;
	/**
	 * Create the panel.
	 */
	public WizardPanel(CyServiceRegistrar registrar, List<WizardStep<DATA>> steps, String title, DATA data) {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,}));
		
		lblTitle = new JLabel("Title");
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 14));
		add(lblTitle, "2, 2, 3, 1");
		
		btnCancel = new JButton("Cancel");
		add(btnCancel, "6, 2");
		btnCancel.addActionListener(evt -> closeWizard());
		
		btnPrevious = new JButton("Previous");
		add(btnPrevious, "4, 6");
		btnPrevious.addActionListener(evt -> previousStep());
		
		btnNext = new JButton("Next");
		add(btnNext, "6, 6");
		btnNext.addActionListener(evt -> nextStep());

		this.registrar = registrar;
		this.steps = steps;
		this.title = title;
		this.data = data;
				
		processingPanel = new ProcessingPanel();
		stepComponentConstraints = new CellConstraints(2, 4, 5, 1);
		
		if(!java.beans.Beans.isDesignTime())
		{
			registrar.getService(RememberValueService.class).loadProperties(data);			
			steps.forEach(step -> step.setData(data, registrar));
			steps.forEach(step -> step.wizardStarted());
			
			setShownStepIndex(0, true);
		}
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	private WizardStep<DATA> getShownStep()
	{
		return steps.get(shownStepIndex);
	}
	
	protected void setShownStepIndex(int index, boolean runBeforeStepProcessing)
	{
		remove(getShownStep().getComponent());
		shownStepIndex = index;	
		
		if(shownStepIndex >= steps.size() -1)
		{
			btnNext.setText("Finish");
		}
		else
		{
			btnNext.setText("Next");
		}
		
		if(shownStepIndex == 0)
		{
			btnPrevious.setEnabled(false);
		}
		else
		{
			btnPrevious.setEnabled(true);
		}
		
		lblTitle.setText("Step " + (shownStepIndex + 1) + ": " + getShownStep().getStepName());
		
		if(runBeforeStepProcessing)
		{
			add(processingPanel, stepComponentConstraints);
			revalidate();
			repaint();
			
			registrar.getService(DialogTaskManager.class).execute(new TaskIterator(
					new AbstractTask() {
						
						@Override
						public void run(TaskMonitor taskMonitor) throws Exception {
							try {
								getShownStep().beforeStep(taskMonitor);
								remove(processingPanel);
								add(getShownStep().getComponent(), stepComponentConstraints);
							
								revalidate();
								repaint();		
								
							}
							catch(Exception ex)
							{
								if(shownStepIndex > 0)
								{
									setShownStepIndex(shownStepIndex - 1,  false);
								}
								else
								{
									closeWizard();
								}
								throw ex;
							}
						}
					}
					));
		}
		else
		{
			add(getShownStep().getComponent(), stepComponentConstraints);			
			revalidate();
			repaint();					
		}
	}
	
	protected void previousStep()
	{
		if(shownStepIndex > 0)
		{
			setShownStepIndex(shownStepIndex - 1, false); 
		}
	}
	
	protected void nextStep()
	{
		registrar.getService(RememberValueService.class).saveProperties(data);
		
		StringBuilder msgBuilder = new StringBuilder();
		switch (getShownStep().validate(msgBuilder))
		{
			case INVALID:
			{
				JOptionPane.showMessageDialog(this, msgBuilder.toString(), "Validation error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			case REQUEST_CONFIRMATION:
			{
				int chosenValue = JOptionPane.showConfirmDialog(this, msgBuilder.toString(), "Confirmation requested", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
				if(chosenValue != JOptionPane.YES_OPTION)
				{
					return;
				}
				break;
			}			
			case OK:
			{
				break;
			}
			default:
				throw new IllegalStateException("Unrecognized validation state");								
		}
		
		registrar.getService(DialogTaskManager.class).execute(new TaskIterator(
				new AbstractTask() {
					
					@Override
					public void run(TaskMonitor taskMonitor) throws Exception {
						getShownStep().performStep(taskMonitor);
						if(shownStepIndex < steps.size() - 1)
						{
							setShownStepIndex(shownStepIndex + 1, true);
						}
						else
						{
							closeWizard();			
						}
					}
				}
				));
		
	}
	
	protected void closeWizard()
	{
		steps.forEach(step -> step.wizardClosed());
		registrar.unregisterAllServices(this);					
	}
}
