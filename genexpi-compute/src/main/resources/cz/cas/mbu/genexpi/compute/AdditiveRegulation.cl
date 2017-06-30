#if !defined(NUM_REGULATORS)
	#error NUM_REGULATORS has to be defined
#endif

#define COMPUTATION_NAME AdditiveRegulation

#define KERNEL_NAME(X) X ## AdditiveRegulation

#if defined(USE_CONSTITUTIVE_EXPRESSION)
	#define CTSW_CONSTITUTIVE_EXPRESSION CTSW_ON
#else
	#define CTSW_CONSTITUTIVE_EXPRESSION CTSW_OFF
#endif


#define K1_VALUE PARAMETER_VALUE(0)
#define K2_VALUE PARAMETER_VALUE(1)
#define B_VALUE PARAMETER_VALUE(2)
#define W_VALUE(regulatorId) PARAMETER_VALUE(3 + (regulatorId))

#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define NUM_PARAMETERS (4 + NUM_REGULATORS)
	#define CONSTITUTIVE_VALUE PARAMETER_VALUE(3 + NUM_REGULATORS)
#else
	#define NUM_PARAMETERS (3 + NUM_REGULATORS)
#endif

//We store the target as the last element of gene profiles
//The rest is only the regulators
#define REGULATOR_INDEX(regulatorId) (regulatorId)
#define REGULATOR_VALUE(regulatorId, time) (regulatorProfiles[(time) * (NUM_REGULATORS) + REGULATOR_INDEX(regulatorId)]) 

#if defined(USE_ERROR_EULER)
    #define ERROR_FUNCTION ErrorEuler
#elif defined(USE_ERROR_RK4)
    #define ERROR_FUNCTION ErrorRK4
#elif defined(USE_ERROR_DERIVATIVE_DIFF)
    #define ERROR_FUNCTION ErrorDerivativeDiff
#else
	#error No error function defined 
#endif

#define CTSW_MODEL_SPECIFIC_PARAMS CTSW_ON
#define CTSW_GLOBAL_MODEL_SPECIFIC_PARAMS CTSW_ON


#define GLOBAL_MODEL_SPECIFIC_PARAMS_DEF \
		global const uint *regulatorIndicesGlobal, global const int *weightConstraintsGlobal

#define GLOBAL_MODEL_SPECIFIC_PARAMS_PASS \
		regulatorIndicesGlobal, weightConstraintsGlobal


#define MODEL_SPECIFIC_PARAMS_DEF \
		local T_Value *regulatorProfiles, local T_Value *profileMaxima, float regularizationWeight, local int *weightConstraints

#define MODEL_SPECIFIC_PARAMS_PASS \
		regulatorProfiles, profileMaxima, regularizationWeight, weightConstraints

#define PREPARE_LOCAL_DATA PrepareLocalData(GLOBAL_BASE_PARAMS_PASS, BASE_PARAMS_PASS, GLOBAL_MODEL_SPECIFIC_PARAMS_PASS, MODEL_SPECIFIC_PARAMS_PASS)


#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define ADDITIONAL_PARAMS_IN_BOUNDS if(CONSTITUTIVE_VALUE < 0) CONSTITUTIVE_VALUE = CONST(0.0001);
#else
	#define ADDITIONAL_PARAMS_IN_BOUNDS
#endif

#define FORCE_PARAMS_IN_BOUNDS \
    if(K1_VALUE < 0) K1_VALUE = CONST(0.1); \
    if(K2_VALUE < 0) K2_VALUE = CONST(0.0001);\
    if(weightConstraints != 0)\
	{ \
    	for(int __regulator = 0; __regulator < NUM_REGULATORS; __regulator++)\
		{\
    		T_Value combinedSignedValue = weightConstraints[__regulator] * W_VALUE(__regulator);\
    		if(combinedSignedValue < 0) { \
    			W_VALUE(__regulator) = weightConstraints[__regulator] * CONST(0.0001);\
    		}\
    	}\
    }\
    ADDITIONAL_PARAMS_IN_BOUNDS


#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define ADDITIONAL_DERIVATIVE_TERM (CONSTITUTIVE_VALUE)
#else
	#define ADDITIONAL_DERIVATIVE_TERM 0
#endif

#define CALCULATE_DERIVATIVE(regulatorWeighedSum, targetValue)  \
		( (K1_VALUE / (1 + exp( -regulatorWeighedSum -B_VALUE))) /*sigmoid*/\
        - (K2_VALUE * (targetValue) ) + ADDITIONAL_DERIVATIVE_TERM)  /*decay*/ 


#define REGULARIZATION_INIT \
	T_Value __reg_minRegulatorInput = INFINITY;\
	T_Value __reg_maxRegulatorInput = -INFINITY;\
	

#define REGULARIZATION_PER_TIME_STEP(time, regulatorSum) \
		__reg_maxRegulatorInput = max(__reg_maxRegulatorInput, -(regulatorSum));\
		__reg_minRegulatorInput = min(__reg_minRegulatorInput, -(regulatorSum));\

#define REGULARIZATION_MAX_EFFECT 10

#if CTSW(CTSW_CONSTITUTIVE_EXPRESSION)
	#define ADDITIONAL_REGULARIZATION_COMPUTE \
		__regValue += RegularizeUniformNormal(CONSTITUTIVE_VALUE, profileMaxima[NUM_REGULATORS]); //last element of profile maxima is target maxima
#else
	#define ADDITIONAL_REGULARIZATION_COMPUTE
#endif

//Regularization bounds are reflected in SampleInitialParams. If regularization changes, so should SampleInitialParams
#define REGULARIZATION_COMPUTE \
	T_Value __regValue = 0; \
	__regValue += RegularizeUniformNormal(K1_VALUE, profileMaxima[NUM_REGULATORS]);\
	for(int __regulator = 0; __regulator < NUM_REGULATORS; __regulator++)\
	{ \
		T_Value w = W_VALUE(__regulator);\
		T_Value maxAbsEffect = fabs(w * profileMaxima[__regulator]);\
		__regValue += RegularizeUniformNormal(maxAbsEffect, REGULARIZATION_MAX_EFFECT);\
	}\
	\
	__reg_maxRegulatorInput -= B_VALUE;\
	__reg_minRegulatorInput -= B_VALUE;\
	if(__reg_maxRegulatorInput < CONST(0.5) || __reg_minRegulatorInput > -CONST(0.5))\
	{\
		T_Value distanceToBorder = min(fabs(__reg_minRegulatorInput + CONST(0.5)),fabs(__reg_maxRegulatorInput - CONST(0.5)));\
		__regValue += RegularizeUniformNormal(distanceToBorder, CONST(3.0));\
	}\
	ADDITIONAL_REGULARIZATION_COMPUTE\
	__regValue *= regularizationWeight;
	
#define REGULARIZATION_VALUE __regValue	
	
	
		

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
	
}

void SampleInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams)
{
    GET_IDS;

    //Tha ranges for initial params reflect regularization bounds - if regularization changes, so should this code.
    
    T_Value maxTarget = 0;
    T_Value regMaxValue[NUM_REGULATORS];
    for(int reg = 0; reg < NUM_REGULATORS; reg++)
    {
    	regMaxValue[reg] = 0;
    }
    
    for(int time = 0; time < numTime - 1; time++)
    {
    	maxTarget = max(maxTarget, TARGET_VALUE(time));
        for(int reg = 0; reg < NUM_REGULATORS; reg++)
        {
        	regMaxValue[reg] = max(regMaxValue[reg], REGULATOR_VALUE(reg, time));
        }
    }
    
    
    const T_Value maxK1 = maxTarget;
    const T_Value maxK2 = CONST(1.0);
    const T_Value maxAbsB = CONST(10.0);
    
    //Initial guess    
    K1_VALUE = XORSHIFT_NEXT_VALUE * maxK1;
    K2_VALUE = XORSHIFT_NEXT_VALUE * maxK2;
    B_VALUE = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsB) - maxAbsB;
   
    for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
    {
    	const T_Value maxAbsW = REGULARIZATION_MAX_EFFECT / regMaxValue[regulator]; 
    	W_VALUE(regulator) = (XORSHIFT_NEXT_VALUE * 2 *  maxAbsW) - maxAbsW;
    }
    
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

        T_Value regulatorSum = 0;
        for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
        {
        	regulatorSum += W_VALUE(regulator) * REGULATOR_VALUE(regulator, time);
        }
        
        const T_Value derivationEstimate = CALCULATE_DERIVATIVE(regulatorSum, TARGET_VALUE(time));
        
        const T_Value diff = derivationEstimate - targetDeriv;
        error += ERROR_FROM_DIFF(diff);        
        
		REGULARIZATION_PER_TIME_STEP(time, regulatorSum);        
    }
    
    REGULARIZATION_COMPUTE;
    return error + REGULARIZATION_VALUE;
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

	
        T_Value regulatorSum = 0;
        for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
        {
        	regulatorSum += W_VALUE(regulator) * REGULATOR_VALUE(regulator, time);
        }
     
        
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
    
    REGULARIZATION_COMPUTE;
    return error + REGULARIZATION_VALUE;
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
    	
        T_Value regulatorSumNow = 0;
        T_Value regulatorSumNext = 0;
        for(int regulator = 0; regulator < NUM_REGULATORS; regulator++)
	    {
        	regulatorSumNow += W_VALUE(regulator) * REGULATOR_VALUE(regulator, time);
        	regulatorSumNext += W_VALUE(regulator) * REGULATOR_VALUE(regulator, time + 1);
	    }
                         	
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
    
    REGULARIZATION_COMPUTE;
    return error + REGULARIZATION_VALUE;
}



/*
void DerivativeErrorGradientUpdate( 
        global const T_Value * geneProfiles, const uint numGenes,  
        const uint numTime, const uint numTasks, const uint numIterations,
		MODEL_SPECIFIC_PARAMS_DEF,
        global T_Value* optimizedParams, float learningRate)
{
    
    GET_IDS;
    
   
    const uint regulatorIndex = regulatorIndices[taskID];
    const uint targetIndex = targetIndices[taskID];
    
    T_Value k1Gradient = 0;
    T_Value k2Gradient = 0;
    T_Value bGradient = 0;
    T_Value wGradient = 0;
    
    for(int time = 1; time < numTime; time++)
    {
        const uint regulatorProfileIndex = time * numGenes + regulatorIndex;
        const uint targetProfileIndex = time * numGenes + targetIndex;

        const T_Value regulatorValue = geneProfiles[regulatorProfileIndex];
        const T_Value targetValue = geneProfiles[targetProfileIndex];
        
        const T_Value targetDeriv = targetValue - geneProfiles[targetProfileIndex - numGenes]; 

        const T_Value exponentialPart = exp( -(W_VALUE * (regulatorValue)) -B_VALUE);
        const T_Value sigmoidDenominator = 1 + exponentialPart;
        const T_Value sigmoidBase = 1 / sigmoidDenominator;
        const T_Value sigmoidScaled = K1_VALUE / sigmoidDenominator;
        
        const T_Value delayAndTarget = - (K2_VALUE * (targetValue) ) - targetDeriv;
        
        k1Gradient += sigmoidBase * (sigmoidBase * K1_VALUE + delayAndTarget);
        k2Gradient += targetValue * (K2_VALUE * targetValue - sigmoidScaled + targetDeriv);
        
        //T_Value sigmoidInputGradient = -K1_VALUE * (sigmoidScaled + delayAndTarget) * sigmoidBase * (1 - sigmoidBase);
        T_Value sigmoidInputGradient = -K1_VALUE * (sigmoidScaled + delayAndTarget) * (exponentialPart / sigmoidDenominator) / sigmoidDenominator;
        
        bGradient -= sigmoidInputGradient;
        wGradient -= regulatorValue * sigmoidInputGradient;
    }       
    
//    if(iterationID == 0)
//    {
//            printf("[%d:%d] Values: %f %f %f %f", taskID, iterationID, K1_VALUE, K2_VALUE, B_VALUE, W_VALUE);
//            printf("\tGradient: %f %f %f %f, rate: %f\n", k1Gradient, k2Gradient, bGradient, wGradient, learningRate);
//    }

    T_Value nF = 10000;
    K1_VALUE += learningRate * (k1Gradient / nF);
    K2_VALUE += learningRate * (k2Gradient / nF);
    B_VALUE += learningRate * (bGradient / nF);
    W_VALUE += learningRate * (wGradient / nF);
}

*/
