//Used in combination with BaseSigmoidRegulation.cl

#define COMPUTATION_NAME AdditiveRegulation

#define KERNEL_NAME(X) X ## AdditiveRegulation

T_Value CalculateRegulatoryInput(BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF, 
		const int time,
        global const T_Value* optimizedParams) {
        	
    GET_IDS 
    T_Value regulatorSum = 0;
    for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
    {
    	regulatorSum += W_VALUE(regulator) * REGULATOR_VALUE(regulator, time);
    }
	return(regulatorSum);
}
