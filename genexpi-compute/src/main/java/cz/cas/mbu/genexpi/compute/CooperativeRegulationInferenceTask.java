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
public class CooperativeRegulationInferenceTask extends BaseDerivativeInferenceTask  {
    private final RegulationType regulationType;
    
	public CooperativeRegulationInferenceTask(int regulator1ID, int regulator2ID, int targetID) {
		super(new int[] { regulator1ID, regulator2ID }, targetID);
		this.regulationType = RegulationType.All;
	}
	
	public CooperativeRegulationInferenceTask(int regulator1ID, int regulator2ID, int targetID, RegulationType regulationType) {
		super(new int[] { regulator1ID, regulator2ID }, targetID);
		this.regulationType = regulationType;
	}

	public RegulationType getRegulationType() {
		return regulationType;
	}
	
}
