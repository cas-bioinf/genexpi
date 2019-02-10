#define W_VALUE(regulatorId) PARAMETER_VALUE(NUM_BASE_PARAMETERS + (regulatorId))

#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define NUM_PARAMETERS (NUM_BASE_PARAMETERS + 1 + NUM_REGULATORS)
	#define CONSTITUTIVE_VALUE PARAMETER_VALUE(NUM_BASE_PARAMETERS + NUM_REGULATORS)
#else
	#define NUM_PARAMETERS (NUM_BASE_PARAMETERS + NUM_REGULATORS)
#endif

#define NUM_WEIGHT_CONSTRAINTS NUM_REGULATORS

void ForceSpecificParamsInBounds(
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {
	
	GET_IDS
	
    if(weightConstraints != 0)
	{ 
    	for(int __regulator = 0; __regulator < NUM_REGULATORS; __regulator++)
		{
    		T_Value constraint = weightConstraints[__regulator];
    		T_Value combinedSignedValue = constraint * W_VALUE(__regulator);
    		if(combinedSignedValue < 0) { 
    			W_VALUE(__regulator) = constraint * CONST(0.0001);
    		}
    	}
    }
}

//Regularization bounds are reflected in SampleInitialParams. If regularization changes, so should SampleInitialParams
T_Value ComputeSpecificRegularization( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global const T_Value* optimizedParams) {

	GET_IDS
		
	T_Value __regValue = 0;
	for(int __regulator = 0; __regulator < NUM_REGULATORS; __regulator++)
	{ 
		T_Value w = W_VALUE(__regulator);
		T_Value maxAbsEffect = fabs(w * profileMaxima[__regulator]);
		__regValue += RegularizeUniformNormal(maxAbsEffect, REGULARIZATION_MAX_EFFECT);
	}
	
	return(__regValue);
}

void SampleSpecificInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {

	GET_IDS
	
    T_Value regMaxValue[NUM_REGULATORS];
    for(int reg = 0; reg < NUM_REGULATORS; reg++)
    {
    	regMaxValue[reg] = 0;
    }
    
    for(int time = 0; time < numTime - 1; time++)
    {
        for(int reg = 0; reg < NUM_REGULATORS; reg++)
        {
        	regMaxValue[reg] = max(regMaxValue[reg], REGULATOR_VALUE(reg, time));
        }
    }
	
    for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
    {
    	const T_Value maxAbsW = REGULARIZATION_MAX_EFFECT / regMaxValue[regulator];
        if(weightConstraints == 0 || weightConstraints[regulator] == 0)
    	{ 
        	W_VALUE(regulator) = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsW) - maxAbsW;
    	} else {		
    		W_VALUE(regulator) = XORSHIFT_NEXT_VALUE *  maxAbsW * weightConstraints[regulator];
    	}
    	
    	
    }
}

void PrepareSpecificLocalData(GLOBAL_BASE_PARAMS_DEF, 
		BASE_PARAMS_DEF, 
		GLOBAL_MODEL_SPECIFIC_PARAMS_DEF, 
		MODEL_SPECIFIC_PARAMS_DEF) {
}