/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.compute;

import java.util.List;

/**
 *
 * @author MBU
 */
public class AdditiveRegulationInferenceTask  {
    private final int[] regulatorIDs;
    private final int targetID;    
	private final RegulationType[] regulationTypes;
    
	public AdditiveRegulationInferenceTask(int regulatorID, int targetID) {
		super();
		this.targetID = targetID;
		this.regulatorIDs = new int[] { regulatorID };
		this.regulationTypes = new RegulationType[] {RegulationType.All};
	}
	
	public AdditiveRegulationInferenceTask(int regulatorID, int targetID, RegulationType regulationType) {
		super();
		this.targetID = targetID;
		this.regulatorIDs = new int[] { regulatorID };
		this.regulationTypes = new RegulationType[] {regulationType};
	}
	
	public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID) {
		super();
		this.targetID = targetID;
		this.regulatorIDs = regulatorIDs;
		this.regulationTypes = new RegulationType[regulatorIDs.length];
		for(int i = 0; i < regulatorIDs.length; i++)
		{
			regulationTypes[i] = RegulationType.All;
		}
	}
	
    public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID, RegulationType[] regulationTypes) {
		super();
		if(regulatorIDs.length != regulationTypes.length)
		{
			throw new IllegalArgumentException("There has to be the same number of weight constraints as regulators.");
		}
		this.regulatorIDs = regulatorIDs;
		this.targetID = targetID;
		this.regulationTypes = regulationTypes;
	}
	
	
	public int getTargetID() {
		return targetID;
	}
	
	public int[] getRegulatorIDs() {
		return regulatorIDs;
	}

	public RegulationType[] getRegulationTypes() {
		return regulationTypes;
	}
   
    
}
