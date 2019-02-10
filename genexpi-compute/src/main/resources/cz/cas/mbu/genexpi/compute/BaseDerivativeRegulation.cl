#if !defined(NUM_REGULATORS)
	#error NUM_REGULATORS has to be defined
#endif


#define PREPARE_LOCAL_DATA PrepareLocalData(GLOBAL_BASE_PARAMS_PASS, BASE_PARAMS_PASS, GLOBAL_MODEL_SPECIFIC_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS)

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


void PrepareLocalData(GLOBAL_BASE_PARAMS_DEF, BASE_PARAMS_DEF, GLOBAL_MODEL_SPECIFIC_PARAMS_DEF, MODEL_SPECIFIC_PARAMS_DEF)
{
	GET_IDS;
	
    if(get_local_id(1) == 0)
    {	
	
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
		
		for(int wc = 0; wc < NUM_WEIGHT_CONSTRAINTS; wc++) {
			if(weightConstraintsGlobal != 0)
			{
				weightConstraints[wc] = weightConstraintsGlobal[taskID * (NUM_WEIGHT_CONSTRAINTS) + wc];
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
    }
    
    PrepareSpecificLocalData(GLOBAL_BASE_PARAMS_PASS, BASE_PARAMS_PASS, GLOBAL_MODEL_SPECIFIC_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS);
	
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

        const T_RegulatoryInput regulatorSum = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time, optimizedParams);
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
    //printf("X\n");
    T_Value valueEstimate = TARGET_VALUE(0);
    //printf("B\n");

       
    REGULARIZATION_INIT;
    for(int time = 0; time < numTime - 1; time++)
    {
	
        const T_RegulatoryInput regulatorSum = CalculateRegulatoryInput(BASE_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS, time, optimizedParams);
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
                
        //printf("E2:%f\n", error);
        //printf("A\n");
    }
        
   
    const T_Value regularizationValue = ComputeReguralization( 
    		BASE_PARAMS_PASS,
    		MODEL_SPECIFIC_PARAMS_PASS,
    		REGULARIZATION_PARAMS_PASS,
            optimizedParams
    		);    
        
    return error + regularizationValue;
}



