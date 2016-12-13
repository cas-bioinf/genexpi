package cz.cas.mbu.cygenexpi.internal.ui.wizard;

import java.beans.Transient;

import org.cytoscape.model.CyNetwork;

import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValue;
import cz.cas.mbu.cygenexpi.RememberValueRecursive;
import cz.cas.mbu.cygenexpi.RememberValue.Type;
import cz.cas.mbu.cygenexpi.internal.ErrorDef;

@RememberAllValues
public class GNWizardData {
	@RememberValue(type=Type.NEVER)
	public CyNetwork selectedNetwork;
	
	public String expressionMappingColumn = "";
	
	@RememberValueRecursive
	public ErrorDef errorDef = new ErrorDef(0.2, 0.5);
	
	public double minFitQuality = 0.8;
	
	public boolean useConstantSynthesis = true;
	

	public static final String CONSTANT_SYNTHESIS_SERIES_NAME = "Constant Synthesis";
	public static final String CONSTANT_SYNTHESIS_COLUMN_NAME = "Constant Synthesis_Idx";
	
	public static final String PREDICTION_SERIES_NAME = "Prediction";
	public static final String PREDICTION_COLUMN_NAME = "Prediction_Idx";
	
}
