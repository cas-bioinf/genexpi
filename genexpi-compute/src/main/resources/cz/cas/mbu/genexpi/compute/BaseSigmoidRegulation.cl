#if !defined(NUM_REGULATORS)
	#error NUM_REGULATORS has to be defined
#endif


#define PREPARE_LOCAL_DATA PrepareLocalData(GLOBAL_BASE_PARAMS_PASS, BASE_PARAMS_PASS, GLOBAL_MODEL_SPECIFIC_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS)



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

//Assumes x is 50% in [0, maxNoPenalty] and in 50% higher than maxNoPenalty with half-normal distribution. All constants are ignored. 
T_Value RegularizeUniformNormal(T_Value xPositive, T_Value maxNoPenalty)
{
	if(xPositive <= maxNoPenalty)
	{
		return 0;
	}
	else
	{
		T_Value diff = (xPositive / maxNoPenalty) - 1;
		return SQUARE(diff);
	}
}


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
	

void PrepareLocalData(GLOBAL_BASE_PARAMS_DEF, BASE_PARAMS_DEF, GLOBAL_MODEL_SPECIFIC_PARAMS_DEF, MODEL_SPECIFIC_PARAMS_DEF)
{
	GET_IDS;
	
	for(int reg = 0; reg < NUM_REGULATORS; reg++)
	{
		CopyProfile(geneProfilesGlobal, numGenes, numTime, regulatorIndicesGlobal[taskID * (NUM_REGULATORS) + reg], regulatorProfiles, NUM_REGULATORS, reg);
		
		profileMaxima[reg] = 0;
		for(int time = 0; time < numTime; time++)
		{
			profileMaxima[reg] = max(profileMaxima[reg], REGULATOR_VALUE(reg, time));
		}
		
		if(weightConstraintsGlobal != 0)
		{
			weightConstraints[reg] = weightConstraintsGlobal[taskID * (NUM_REGULATORS) + reg];
		}
		else 
		{
			weightConstraints = 0;
		}
	}
	profileMaxima[NUM_REGULATORS] = 0; 
    for(int time = 0; time < numTime; time++) 
	{ 
	  profileMaxima[NUM_REGULATORS] = max(profileMaxima[NUM_REGULATORS], TARGET_VALUE(time));     
	}
    
    PrepareSpecificLocalData(GLOBAL_BASE_PARAMS_PASS, BASE_PARAMS_PASS, GLOBAL_MODEL_SPECIFIC_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS);
	
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


T_Value ErrorDerivativeDiff( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global const T_Value* optimizedParams)
{
    GET_IDS;
    
    T_Value error = 0;

    REGULARIZATION_INIT;
    
    //Start at one to have 
    for(int time = 1; time < numTime; time++)
    {
        const T_Value targetDeriv =  TARGET_VALUE(time) - TARGET_VALUE(time - 1);

        const T_Value regulatorSum = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time, optimizedParams);
        const T_Value derivationEstimate = CALCULATE_DERIVATIVE(regulatorSum, TARGET_VALUE(time));
        
        const T_Value diff = derivationEstimate - targetDeriv;
        error += ERROR_FROM_DIFF(diff);        
        
		REGULARIZATION_PER_TIME_STEP(time, regulatorSum);        
    }
    
    const T_Value regularizationValue = ComputeReguralization( 
    		BASE_PARAMS_PASS,
    		MODEL_SPECIFIC_PARAMS_PASS,
    		REGULARIZATION_PARAMS_PASS,
            optimizedParams
    		);    
    return error + regularizationValue;
}

T_Value ErrorEuler( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global const T_Value* optimizedParams)
{
    GET_IDS;
    
      
    T_Value error = 0;
    T_Value valueEstimate = TARGET_VALUE(0);
    
    REGULARIZATION_INIT;
    for(int time = 0; time < numTime - 1; time++)
    {

	
        const T_Value regulatorSum = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time, optimizedParams);
        const T_Value derivativeEstimate = CALCULATE_DERIVATIVE(regulatorSum, valueEstimate);

        if(isinf(derivativeEstimate))
        {
        	return INFINITY;
        }        
        
#if CTSW(CTSW_CUSTOM_TIME_STEP)
        valueEstimate += derivativeEstimate * (CUSTOM_TIME_STEP); //I have unit timesteps
#else
        valueEstimate += derivativeEstimate; //I have unit timesteps
#endif
        
        if(isinf(valueEstimate))
        {
        	return INFINITY;
        }      
        if(isnan(valueEstimate)){
        	return INFINITY;
        }
        REGULARIZATION_PER_TIME_STEP(time, regulatorSum);

        const T_Value diff = valueEstimate - TARGET_VALUE(time + 1);        
        error += ERROR_FROM_DIFF(diff);        
                              
    }
    
    const T_Value regularizationValue = ComputeReguralization( 
    		BASE_PARAMS_PASS,
    		MODEL_SPECIFIC_PARAMS_PASS,
    		REGULARIZATION_PARAMS_PASS,
            optimizedParams
    		);    
    return error + regularizationValue;
}


T_Value ErrorRK4( 
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global const T_Value* optimizedParams)
{
    GET_IDS;
    
     
    T_Value error = 0;
    T_Value valueEstimate = TARGET_VALUE(0);
    
    REGULARIZATION_INIT;
    //Start at one to have 
    for(int time = 0; time < numTime - 1; time++)
    {
    	
        T_Value regulatorSumNow = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time, optimizedParams);
        T_Value regulatorSumNext = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time + 1, optimizedParams);

        //TODO consider model-specific interpolation 
        const T_Value regulatorSumMid = CONST(0.5) * (regulatorSumNow + regulatorSumNext);
       
#if CTSW(CTSW_CUSTOM_TIME_STEP)        
        //RK4 coefficients, using custom time step
        const T_Value k1 = CALCULATE_DERIVATIVE(regulatorSumNow,  valueEstimate);
        const T_Value k2 = CALCULATE_DERIVATIVE(regulatorSumMid,  valueEstimate + ((CUSTOM_TIME_STEP) * CONST(0.5) * k1));
        const T_Value k3 = CALCULATE_DERIVATIVE(regulatorSumMid,  valueEstimate + ((CUSTOM_TIME_STEP) * CONST(0.5) * k2));
        const T_Value k4 = CALCULATE_DERIVATIVE(regulatorSumNext, valueEstimate + ((CUSTOM_TIME_STEP) * k3));
        
        valueEstimate +=  (CUSTOM_TIME_STEP) * (k1 + 2*k2 + 2*k3 + k4) / 6;
#else
        //RK4 coefficients, using timeStep = 1
        const T_Value k1 = CALCULATE_DERIVATIVE(regulatorSumNow,  valueEstimate);
        const T_Value k2 = CALCULATE_DERIVATIVE(regulatorSumMid,  valueEstimate + (CONST(0.5) * k1));
        const T_Value k3 = CALCULATE_DERIVATIVE(regulatorSumMid,  valueEstimate + (CONST(0.5) * k2));
        const T_Value k4 = CALCULATE_DERIVATIVE(regulatorSumNext, valueEstimate + k3);
        
        valueEstimate +=  (k1 + 2*k2 + 2*k3 + k4) / CONST(6.0);
#endif

        const T_Value diff = valueEstimate - TARGET_VALUE(time + 1);
        error += ERROR_FROM_DIFF(diff);
        
        REGULARIZATION_PER_TIME_STEP(time, regulatorSumNow);
        
    }
    
    const T_Value regularizationValue = ComputeReguralization( 
    		BASE_PARAMS_PASS,
    		MODEL_SPECIFIC_PARAMS_PASS,
    		REGULARIZATION_PARAMS_PASS,
            optimizedParams
    		);    
    return error + regularizationValue;
}
