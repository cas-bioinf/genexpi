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
public class InferenceResult {
    private double[] parameters;
    private double error;


	public InferenceResult(double[] parameters, double error) {
		super();
		this.parameters = parameters;
		this.error = error;
	}


	public double[] getParameters() {
		return parameters;
	}


	public double getError() {
		return error;
	}

 
    
}
