void ForceParamsInBounds(
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams) {
	
	GET_IDS
	
    if(K1_VALUE < 0) K1_VALUE = CONST(0.1); 
    if(K2_VALUE < 0) K2_VALUE = CONST(0.0001);
    ForceSpecificParamsInBounds(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, optimizedParams);
    
#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	if(CONSTITUTIVE_VALUE < 0) CONSTITUTIVE_VALUE = CONST(0.0001);
#endif
}

#define REGULARIZATION_INIT \
	T_Value __reg_minRegulatorInput = INFINITY;\
	T_Value __reg_maxRegulatorInput = -INFINITY;\
	

#define REGULARIZATION_PER_TIME_STEP(time, regulatorSum) \
		__reg_maxRegulatorInput = max(__reg_maxRegulatorInput, -(regulatorSum));\
		__reg_minRegulatorInput = min(__reg_minRegulatorInput, -(regulatorSum));\

#define REGULARIZATION_PARAMS_PASS __reg_minRegulatorInput, __reg_maxRegulatorInput


//Regularization bounds are reflected in SampleInitialParams. If regularization changes, so should SampleInitialParams
T_Value ComputeReguralization( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
		T_Value __reg_minRegulatorInput, T_Value __reg_maxRegulatorInput,
        global const T_Value* optimizedParams
		) {
	
	GET_IDS

	T_Value __regValue = 0; 
	__regValue += RegularizeUniformNormal(K1_VALUE, profileMaxima[NUM_REGULATORS]);
	
	__regValue += ComputeSpecificRegularization(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, optimizedParams);
	
	__reg_maxRegulatorInput -= B_VALUE;
	__reg_minRegulatorInput -= B_VALUE;
	if(__reg_maxRegulatorInput < CONST(0.5) || __reg_minRegulatorInput > -CONST(0.5))
	{
		T_Value distanceToBorder = min(fabs(__reg_minRegulatorInput + CONST(0.5)),fabs(__reg_maxRegulatorInput - CONST(0.5)));
		__regValue += RegularizeUniformNormal(distanceToBorder, CONST(3.0));
	}
	
#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	//last element of profile maxima is target maxima
	__regValue += RegularizeUniformNormal(CONSTITUTIVE_VALUE, profileMaxima[NUM_REGULATORS]); 
#endif
	
	__regValue *= regularizationWeight;
	
	return(__regValue);
}

void SampleInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams)
{
    GET_IDS;

    //Tha ranges for initial params reflect regularization bounds - if regularization changes, so should this code.
    
    T_Value maxTarget = 0;
    for(int time = 0; time < numTime - 1; time++)
    {
    	maxTarget = max(maxTarget, TARGET_VALUE(time));
    }
    
    const T_Value maxK1 = maxTarget;
    const T_Value maxK2 = CONST(1.0);
    const T_Value maxAbsB = CONST(10.0);
    
    //Initial guess    
    K1_VALUE = XORSHIFT_NEXT_VALUE * maxK1;
    K2_VALUE = XORSHIFT_NEXT_VALUE * maxK2;
    B_VALUE = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsB) - maxAbsB;
   
    SampleSpecificInitialParams(XORSHIFT_PARAMS_PASS, BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, optimizedParams);
    
#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
    CONSTITUTIVE_VALUE = XORSHIFT_NEXT_VALUE * maxTarget;
#endif    
}
