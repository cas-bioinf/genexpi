package cz.cas.mbu.genexpi.compute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InferenceModel {
	/*
	NoRegulator("NoRegulator", null, new String[] {"synthesis", "decay"}, null),
	OneRegulator("AdditiveRegulation", EErrorFunction.Euler, new String[] { "k1","k2","b","w"}, new String[][] { {"NUM_REGULATORS", "1"}}), 
	TwoRegulators("AdditiveRegulation", EErrorFunction.Euler, new String[] { "k1","k2","b","w1","w2"}, new String[][] { {"NUM_REGULATORS", "2"}}),
	OneRegulatorConstitutive("AdditiveRegulation", EErrorFunction.Euler, new String[] { "k1","k2","b","w","const"}, new String[][] { {"NUM_REGULATORS", "1"}, {"USE_CONSTITUTIVE_EXPRESSION", ""}}), 
	;
	*/

	public enum Family {NoRegulator, AdditiveRegulation, CooperativeRegulation};
	
	public static final String NO_REGULATOR_SYNTHESIS_PARAM_NAME = "synthesis";
	public static final String NO_REGULATOR_DECAY_PARAM_NAME = "decay";
	
	public static final String ADDITIVE_MAX_SYNTH_PARAM_NAME = "k1";
	public static final String ADDITIVE_DECAY_PARAM_NAME = "k2";
	public static final String ADDITIVE_BIAS_PARAM_NAME = "b";
	public static final String ADDITIVE_CONSTITUTIVE_PARAM_NAME = "const";
	
	private final Family family;
	
	private final String kernelName;
	
	private final String[] parameters;

	private final String [][] additionalDefines;

	private final String description;

	public static final InferenceModel NO_REGULATOR = new InferenceModel(Family.NoRegulator, "NoRegulator",new String[] {NO_REGULATOR_SYNTHESIS_PARAM_NAME, NO_REGULATOR_DECAY_PARAM_NAME}, null, "NoRegulator");
	
	
	public static String getAdditiveRegulatorWeightParamName(int regulator, int numRegulators)
	{
		if(numRegulators == 1)
		{
			return "w";
		}
		else
		{
			return "w" + Integer.toString(regulator + 1);
		}				
	}
	
	public static InferenceModel createAdditiveRegulationModel(int numRegulators)
	{
		return createAdditiveRegulationModel(numRegulators, false);
	}
	
	public static InferenceModel createAdditiveRegulationModel(int numRegulators, boolean useConstitutiveExpression)
	{
		List<String> parameters = new ArrayList<>();
		parameters.add(ADDITIVE_MAX_SYNTH_PARAM_NAME);
		parameters.add(ADDITIVE_DECAY_PARAM_NAME);
		parameters.add(ADDITIVE_BIAS_PARAM_NAME);

		for(int i = 0; i < numRegulators; i++)
		{
			parameters.add(getAdditiveRegulatorWeightParamName(i, numRegulators));
		}			
		
		List<String[]> additionalDefines = new ArrayList<>();
		additionalDefines.add(new String[]  {"NUM_REGULATORS", Integer.toString(numRegulators)});
		
		if(useConstitutiveExpression)
		{
			parameters.add(ADDITIVE_CONSTITUTIVE_PARAM_NAME);
			additionalDefines.add(new String[] {"USE_CONSTITUTIVE_EXPRESSION", ""});
		}
		String description = "Additive-" + Integer.toString(numRegulators) + "Reg" + (useConstitutiveExpression ? "-Constitutive" : "");
		return new InferenceModel(Family.AdditiveRegulation, "AdditiveRegulation", parameters, additionalDefines, description);
	}
	
	private InferenceModel(Family family, String kernelName, List<String> parameters,
			List<String[]> additionalDefines, String description) {
		this.family = family;
		this.kernelName = kernelName;
		this.parameters = new String[parameters.size()];
		parameters.toArray(this.parameters);
		this.additionalDefines = new String[additionalDefines.size()][];
		additionalDefines.toArray(this.additionalDefines);
		this.description = description;
	}
	
	private InferenceModel(Family family, String kernelName, String[] parameters,
			String[][] additionalDefines, String description) {
		this.family = family;
		this.kernelName = kernelName;
		this.parameters = parameters;
		this.additionalDefines = additionalDefines;
		this.description = description;
	}

	
	
	@Override
	public String toString() {
		return description;
	}

	public String getKernelName(String kernelBaseName)
	{
		return kernelBaseName + kernelName;
	}
	
	public String[] getModelSources()
	{
		switch(family) {
			case AdditiveRegulation: {
				return new String[] { "BaseSigmoidDefinitions.clh", "AdditiveRegulation.cl", "BaseSigmoidRegulation.cl" };
			}
			case CooperativeRegulation: {
				return new String[] { "BaseSigmoidDefinitions.clh", "CooperativeRegulation.cl", "BaseSigmoidRegulation.cl" };				
			}
			default: {
				return new String[] { kernelName + ".cl" };				
			}
		}
		
	}

	public int getNumParams() {
		return parameters.length;
	}

	public String[] getParameterNames() {
		return parameters;
	}

	public int getParameterIndex(String parameterName)
	{
		int index = Arrays.asList(parameters).indexOf(parameterName);
		if(index < 0)
		{
			throw new IllegalArgumentException("Unknown parameter: " + parameterName);
		}
		return index;
	}

	public String[][] getAdditionalDefines() {
		return additionalDefines;
	}

	public Family getFamily() {
		return family;
	}		
	
}
