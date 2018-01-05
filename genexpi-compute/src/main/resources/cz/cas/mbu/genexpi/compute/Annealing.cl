#define PARAM_ARRAY_ACCESS_DEBUG(i, sourceArray) ((i) >= NUM_PARAMETERS ? NAN : (sourceArray)[i])

kernel void KERNEL_NAME(Annealing)(XORSHIFT_PARAMS_DEF,
		GLOBAL_BASE_PARAMS_DEF,
		BASE_PARAMS_DEF,
#if CTSW(CTSW_GLOBAL_MODEL_SPECIFIC_PARAMS)		
		GLOBAL_MODEL_SPECIFIC_PARAMS_DEF,
#endif		
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
        MODEL_SPECIFIC_PARAMS_DEF,
#endif
        //output params
        global T_Value *optimizedParams, global T_Value *error
) {    
    GET_IDS;

    CHECK_IDS;  
    //pridat divnej vypocet
	CopyProfile(geneProfilesGlobal, numGenes, numTime, targetIndicesGlobal[taskID], targetProfile, 1, 0); //the target is a single profile - stride 1, offset 0
	PREPARE_LOCAL_DATA;
    	
    barrier(CLK_LOCAL_MEM_FENCE); //wait until all the local data is ready
    
    XorShiftInit(xorShiftCounters, numTasks, numIterations);

    SampleInitialParams(XORSHIFT_PARAMS_PASS,
    		BASE_PARAMS_PASS,
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
			MODEL_SPECIFIC_PARAMS_PASS,
#endif
			optimizedParams);
    
    ForceParamsInBounds(BASE_PARAMS_PASS, 
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
			MODEL_SPECIFIC_PARAMS_PASS,
#endif
    		optimizedParams);
    
    const T_Value coolMultiplier = CONST(0.8);
    const T_Value initTemperature = CONST(1.0);
    const uint maxConsecutiveRejections = 1000;
    const uint maxSuccesses = 20;
    const uint maxTries = 300;
    const T_Value stopTemperature = CONST(1e-6);
    
    T_Value selectedParams[NUM_PARAMETERS];
    T_Value bestParams[NUM_PARAMETERS];
    for(int i = 0; i < NUM_PARAMETERS; i++)
    {
        selectedParams[i] = PARAMETER_VALUE(i);
        bestParams[i] = PARAMETER_VALUE(i);
    }
    
    
    
    T_Value selectedEnergy = ERROR_FUNCTION(
    		BASE_PARAMS_PASS,
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
			MODEL_SPECIFIC_PARAMS_PASS,
#endif
            optimizedParams);
    
    if (isnan(selectedEnergy))
    {
#if CTSW(CHECK_NAN)    
    	printf("Initial NaN\n");
#endif
    	selectedEnergy = INFINITY;
    }
       
    T_Value bestEnergy = selectedEnergy;
    
#if CTSW(DEBUG)    
                if(iterationID == 0)
                {
                    printf(COMPUTATION_NAME_STRING " [%d:%d] Init. Energy: %f [ %f %f %f %f  %f ]\n", taskID, iterationID, sqrt(bestEnergy / numTime), PARAM_ARRAY_ACCESS_DEBUG(0, bestParams), PARAM_ARRAY_ACCESS_DEBUG(1, bestParams), PARAM_ARRAY_ACCESS_DEBUG(2, bestParams), PARAM_ARRAY_ACCESS_DEBUG(3, bestParams), PARAM_ARRAY_ACCESS_DEBUG(4, bestParams));
                }
#endif                    
    
    
    uint numRejections = 0;
    uint numSuccesses = 0;
    uint numTries = 0;
    T_Value temperature = initTemperature;
    
    while(true)
    {
        numTries++;
        
        for(int i = 0; i < NUM_PARAMETERS; i++)
        {
            PARAMETER_VALUE(i) = selectedParams[i];
        }
    
        if(numTries >= maxTries || numSuccesses >= maxSuccesses)
        {
            if(temperature < stopTemperature || numRejections >= maxConsecutiveRejections)
            {
                break;
            }
            else
            {
                temperature *= coolMultiplier;
                numTries = 1;
                numSuccesses = 1;
            }
        }
        
        uint indexToChange = XorShiftNext(XORSHIFT_PARAMS_PASS, numTasks, numIterations) % (NUM_PARAMETERS);
        T_Value oldValue = PARAMETER_VALUE(indexToChange);        
        T_Value randomValue = XORSHIFT_RANDN;
        if (fabs(oldValue) < 1e-5)
        {
            PARAMETER_VALUE(indexToChange) = oldValue + (randomValue * CONST(1e-5));                    	
        }
        else
        {
            PARAMETER_VALUE(indexToChange) = oldValue + (oldValue * randomValue * CONST(0.5));                    	
        }
               
        ForceParamsInBounds(BASE_PARAMS_PASS, 
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
    			MODEL_SPECIFIC_PARAMS_PASS,
#endif
        		optimizedParams);
        
#if CTSW(DEBUG) 
        //if(iterationID == 0)
         //       printf("[%d:%d] Index to change: %d, newValue: %f\n", taskID, iterationID, indexToChange, PARAMETER_VALUE(indexToChange));
#endif                    
        
        T_Value currentEnergy = ERROR_FUNCTION(    		
        		BASE_PARAMS_PASS,
#if CTSW(CTSW_MODEL_SPECIFIC_PARAMS)		
    			MODEL_SPECIFIC_PARAMS_PASS,
#endif
                optimizedParams);
                
#if CTSW(DEBUG)    
              if(iterationID == 0 && numTries % 5 == 0)
                	printf(COMPUTATION_NAME_STRING " [%d:%d] Energy: %f [ %f %f %f %f  %f ]\n", taskID, iterationID, sqrt(currentEnergy / numTime), PARAM_ACCESS_DEBUG(0, optimizedParams), PARAM_ACCESS_DEBUG(1, optimizedParams), PARAM_ACCESS_DEBUG(2, optimizedParams), PARAM_ACCESS_DEBUG(3, optimizedParams), PARAM_ACCESS_DEBUG(4, optimizedParams));
#endif                    
        
        if (isnan(currentEnergy))
        {
        	currentEnergy = INFINITY;
#if CTSW(CHECK_NAN)    
        	printf("currentEnergy NaN:" COMPUTATION_NAME_STRING " [%d:%d] Energy: [ %f %f %f %f  %f ]\n", taskID, iterationID, PARAM_ACCESS_DEBUG(0, optimizedParams), PARAM_ACCESS_DEBUG(1, optimizedParams), PARAM_ACCESS_DEBUG(2, optimizedParams), PARAM_ACCESS_DEBUG(3, optimizedParams), PARAM_ACCESS_DEBUG(4, optimizedParams));
#endif        	
        }
        
        if (selectedEnergy > currentEnergy )
        {
            for(int i = 0; i < NUM_PARAMETERS; i++)
            {
                selectedParams[i] = PARAMETER_VALUE(i);
            }
            
            
            selectedEnergy = currentEnergy;
            numSuccesses++;
            numRejections = 0;

            if (bestEnergy > currentEnergy)
            {
                for(int i = 0; i < NUM_PARAMETERS; i++)
                {
                    bestParams[i] = selectedParams[i];
                }                
                bestEnergy = currentEnergy;
#if CTSW(DEBUG)    
              if(iterationID == 0)
                	printf(COMPUTATION_NAME_STRING " [%d:%d] New best. Energy: %f [ %f %f %f %f  %f ]\n", taskID, iterationID, sqrt(bestEnergy / numTime), PARAM_ARRAY_ACCESS_DEBUG(0, bestParams), PARAM_ARRAY_ACCESS_DEBUG(1, bestParams), PARAM_ARRAY_ACCESS_DEBUG(2, bestParams), PARAM_ARRAY_ACCESS_DEBUG(3, bestParams), PARAM_ARRAY_ACCESS_DEBUG(4, bestParams));
#endif                    
            }
            
            
        }
        else
        {
            if( XORSHIFT_NEXT_VALUE < exp( (selectedEnergy - currentEnergy) / temperature ))
            {
                for(int i = 0; i < NUM_PARAMETERS; i++)
                {
                    selectedParams[i] = PARAMETER_VALUE(i);
                }
                selectedEnergy = currentEnergy;
                numSuccesses++;
            }
            else
            {
                numRejections++;
            }
        }        
    }

    for(int i = 0; i < NUM_PARAMETERS; i++)
    {
        PARAMETER_VALUE(i) = bestParams[i];
    }    
    error[taskID * numIterations + iterationID] = sqrt(bestEnergy / numTime);
}

