package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.util.Arrays;
import java.util.List;

public class GNWizard {
	public static String TITLE = "GN Wizard";
	public static List<WizardStep<GNWizardData>> getSteps()
	{
		return Arrays.asList(
				new SelectTimeSeriesStep(), 
				new EnterErrorCheckingStep(),
				new PredictAndApproveNodeTagsStep(),
				new PredictAndApproveEdgeTagsStep()
				);
	}
}
