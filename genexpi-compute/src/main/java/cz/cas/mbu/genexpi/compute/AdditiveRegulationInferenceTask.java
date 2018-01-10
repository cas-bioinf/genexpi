/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.compute;

/**
 *
 * @author MBU
 */
public class AdditiveRegulationInferenceTask extends BaseSigmoidInferenceTask  {
    private final RegulationType[] regulationTypes;
    
	public AdditiveRegulationInferenceTask(int regulatorID, int targetID) {
		super(new int[] { regulatorID }, targetID);
		this.regulationTypes = new RegulationType[] {RegulationType.All};
	}
	
	public AdditiveRegulationInferenceTask(int regulatorID, int targetID, RegulationType regulationType) {
		super(new int[] { regulatorID }, targetID);
		this.regulationTypes = new RegulationType[] {regulationType};
	}
	
	public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID) {
		super(regulatorIDs, targetID);
		this.regulationTypes = new RegulationType[regulatorIDs.length];
		for(int i = 0; i < regulatorIDs.length; i++)
		{
			regulationTypes[i] = RegulationType.All;
		}
	}
	
    public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID, RegulationType[] regulationTypes) {
		super(regulatorIDs, targetID);
		if(regulatorIDs.length != regulationTypes.length)
		{
			throw new IllegalArgumentException("There has to be the same number of weight constraints as regulators.");
		}
		this.regulationTypes = regulationTypes;
	}
	
	
	public RegulationType[] getRegulationTypes() {
		return regulationTypes;
	}
   
    
}
