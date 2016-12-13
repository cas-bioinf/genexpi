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
	private final int[] weightConstraints;
    
	public AdditiveRegulationInferenceTask(int regulatorID, int targetID) {
		super();
		this.targetID = targetID;
		this.regulatorIDs = new int[] { regulatorID };
		this.weightConstraints = new int[] {0};
	}
	
	public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID) {
		super();
		this.targetID = targetID;
		this.regulatorIDs = regulatorIDs;
		this.weightConstraints = new int[regulatorIDs.length];
		for(int i = 0; i < regulatorIDs.length; i++)
		{
			weightConstraints[i] = 0;
		}
	}
	
    public AdditiveRegulationInferenceTask(int[] regulatorIDs, int targetID, int[] weightConstraints) {
		super();
		if(regulatorIDs.length != weightConstraints.length)
		{
			throw new IllegalArgumentException("There has to be the same number of weight constraints as regulators.");
		}
		this.regulatorIDs = regulatorIDs;
		this.targetID = targetID;
		this.weightConstraints = weightConstraints;
	}
	
	
	public int getTargetID() {
		return targetID;
	}
	
	public int[] getRegulatorIDs() {
		return regulatorIDs;
	}

	public int[] getWeightConstraints() {
		return weightConstraints;
	}
   
    
}
