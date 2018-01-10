//Used in combination with BaseSigmoidRegulation.cl

#define COMPUTATION_NAME CooperativeRegulation

#define KERNEL_NAME(X) X ## CooperativeRegulation

#define W_VALUE PARAMETER_VALUE(NUM_BASE_PARAMETERS)
#define EQ_VALUE PARAMETER_VALUE(NUM_BASE_PARAMETERS + 1)

#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define NUM_PARAMETERS (NUM_BASE_PARAMETERS + 3)
	#define CONSTITUTIVE_VALUE PARAMETER_VALUE(NUM_BASE_PARAMETERS + 2)
#else
	#define NUM_PARAMETERS (NUM_BASE_PARAMETERS + 2)
#endif

void ForceSpecificParamsInBounds(
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {
	
	GET_IDS
	
	if(EQ_VALUE <= 0) {
		EQ_VALUE = CONST(0.0001);
	}
	
    if(weightConstraints != 0)
	{ 
		T_Value constraint = weightConstraints[taskID];
		T_Value combinedSignedValue = constraint * W_VALUE;
		if(combinedSignedValue < 0) { 
			W_VALUE = constraint * CONST(0.0001);
		}
    }	
}


//Regularization bounds are reflected in SampleInitialParams. If regularization changes, so should SampleInitialParams
T_Value ComputeSpecificRegularization( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global const T_Value* optimizedParams) {
/*
 * TODO: Maybe do a similar regularization, but for the combined profile
	GET_IDS
		
	
	T_Value __regValue;
	for(int __regulator = 0; __regulator < NUM_REGULATORS; __regulator++)
	{ 
		T_Value w = W_VALUE(__regulator);
		T_Value maxAbsEffect = fabs(w * profileMaxima[__regulator]);
		__regValue += RegularizeUniformNormal(maxAbsEffect, REGULARIZATION_MAX_EFFECT);
	}
	
	return(__regValue);
	*/
}

void SampleSpecificInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {

	GET_IDS
	
    T_Value regMaxValue = 0;
    
    for(int time = 0; time < numTime - 1; time++)
    {
       	regMaxValue = max(regMaxValue, min(REGULATOR_VALUE(0, time),REGULATOR_VALUE(1, time)));
    }
	
	const T_Value maxAbsW = REGULARIZATION_MAX_EFFECT / regMaxValue; 
	W_VALUE = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsW) - maxAbsW;
	EQ_VALUE = min(atanh(XORSHIFT_NEXT_VALUE),CONST(10000.0));
}

T_Value CalculateRegulatoryInput(BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF, 
		const int time,
        global const T_Value* optimizedParams) {
        	
    GET_IDS
	T_Value reg0 = (REGULATOR_VALUE(0, time));
    T_Value reg1 = (REGULATOR_VALUE(1, time));
    T_Value eq = EQ_VALUE;
    T_Value regDiff = reg0 - reg1;
	T_Value discriminant = (regDiff * regDiff) / (eq * eq) + 2 * ((reg0 + reg1) / eq) + 1;  
	T_Value freeReg1 = (-regDiff / eq) - CONST(1.0) + sqrt(discriminant) * eq * CONST(0.5);
	T_Value complex = reg1 - freeReg1; 
	return(complex);
}

void PrepareSpecificLocalData(GLOBAL_BASE_PARAMS_DEF, 
		BASE_PARAMS_DEF, 
		GLOBAL_MODEL_SPECIFIC_PARAMS_DEF, 
		MODEL_SPECIFIC_PARAMS_DEF) {
}