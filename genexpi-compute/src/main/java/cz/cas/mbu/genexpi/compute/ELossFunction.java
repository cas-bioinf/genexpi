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
public enum ELossFunction {
    Squared("USE_LOSS_SQUARED"),Abs("USE_LOSS_ABS");
    
    private final String macro;

    private ELossFunction(String macro) {
        this.macro = macro;
    }

    public String getMacro() {
        return macro;
    }      
}
