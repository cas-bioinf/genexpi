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
public enum EErrorFunction {
    Euler("USE_ERROR_EULER"),DerivativeDiff("USE_ERROR_DERIVATIVE_DIFF");
    
    private final String macro;

    private EErrorFunction(String macro) {
        this.macro = macro;
    }

    public String getMacro() {
        return macro;
    }      
}
