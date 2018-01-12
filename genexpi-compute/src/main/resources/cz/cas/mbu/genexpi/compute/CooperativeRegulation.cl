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

#define NUM_WEIGHT_CONSTRAINTS 1

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
		T_Value constraint = weightConstraints[0];
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
	
	GET_IDS
	T_Value __regValue;
	T_Value maxMaxProfile = max(profileMaxima[0], profileMaxima[1]);
	__regValue += RegularizeUniformNormal(EQ_VALUE, maxMaxProfile * 10);
	
	T_Value minMaxProfile = min(profileMaxima[0], profileMaxima[1]);
	T_Value maxAbsEffect = fabs(W_VALUE * minMaxProfile);
	__regValue += RegularizeUniformNormal(maxAbsEffect, REGULARIZATION_MAX_EFFECT);
	
	return(__regValue);
}

void SampleSpecificInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {

	GET_IDS
	
    T_Value compoundMaxValue = 0;
	T_Value regulatorMaxValue = 0;
    
    for(int time = 0; time < numTime - 1; time++)
    {
    	compoundMaxValue = max(compoundMaxValue, min(REGULATOR_VALUE(0, time),REGULATOR_VALUE(1, time)));
    	regulatorMaxValue = max(regulatorMaxValue, REGULATOR_VALUE(0, time));
    	regulatorMaxValue = max(regulatorMaxValue, REGULATOR_VALUE(1, time));
    }
	
	const T_Value maxAbsW = REGULARIZATION_MAX_EFFECT / compoundMaxValue;
    if(weightConstraints == 0 || weightConstraints[0] == 0)
	{ 
    	W_VALUE = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsW) - maxAbsW;
	} else {		
		W_VALUE = XORSHIFT_NEXT_VALUE * maxAbsW * weightConstraints[0];
	}
    
    T_Value maxEq = regulatorMaxValue;
    EQ_VALUE = XORSHIFT_NEXT_VALUE * maxEq;
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
	T_Value freeReg1 = ((-regDiff / eq) - CONST(1.0) + sqrt(discriminant)) * eq * CONST(0.5);
	T_Value complex = reg1 - freeReg1;
	return(W_VALUE * complex);
}

void PrepareSpecificLocalData(GLOBAL_BASE_PARAMS_DEF, 
		BASE_PARAMS_DEF, 
		GLOBAL_MODEL_SPECIFIC_PARAMS_DEF, 
		MODEL_SPECIFIC_PARAMS_DEF) {
}