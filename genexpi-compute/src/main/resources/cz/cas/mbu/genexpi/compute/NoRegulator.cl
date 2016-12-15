
#define COMPUTATION_NAME NoRegulator

#define KERNEL_NAME(X) X ## NoRegulator


#define NUM_PARAMETERS 2    
#define SYNTHESIS_VALUE PARAMETER_VALUE(0)
#define DECAY_VALUE PARAMETER_VALUE(1)

#define CTSW_MODEL_SPECIFIC_PARAMS CTSW_OFF
#define CTSW_GLOBAL_MODEL_SPECIFIC_PARAMS CTSW_OFF



#define PREPARE_LOCAL_DATA




#define FORCE_PARAMS_IN_BOUNDS \
    if(SYNTHESIS_VALUE < 0) SYNTHESIS_VALUE = CONST(0.000001); \
    if(DECAY_VALUE < CONST(0.00001)) DECAY_VALUE = CONST(0.0001); //decay has to be positive 

#define ERROR_FUNCTION ErrorNoRegulator

T_Value ErrorNoRegulator( 
		BASE_PARAMS_DEF, 
        global const T_Value* optimizedParams)
{
    GET_IDS;
    
       
    T_Value error = 0;
    T_Value initialValue = TARGET_VALUE(0); //this is the 0th element of the series

    T_Value basalSynthesis = SYNTHESIS_VALUE;
    T_Value decay = DECAY_VALUE;

    T_Value derivativeAtZero = basalSynthesis - (decay * initialValue);
    T_Value signDerivativeAtZero = (derivativeAtZero > 0) ? 1 : -1; //sign of the derivative
    T_Value constantFactor = fabs(derivativeAtZero) / (-decay);
    
    T_Value ratio = basalSynthesis / decay;
    
#if CTSW(CTSW_CUSTOM_TIME_STEP)
    	const T_Value timeStep = CUSTOM_TIME_STEP;
#else
    	const T_Value timeStep = 1;
#endif
    	
    //Start at one (initial condition ensures that error at 0th point is 0)
    for(int time = 1; time < numTime; time++)
    {
        T_Value value = signDerivativeAtZero * (constantFactor * exp(-decay * time * timeStep) + ratio);
      
        const T_Value diff = value - TARGET_VALUE(time);
        error += ERROR_FROM_DIFF(diff);        

    }
    
    return error;
}


void SampleInitialParams(XORSHIFT_PARAMS_DEF,
		BASE_PARAMS_DEF,
        global T_Value* optimizedParams)
{
    GET_IDS;

    T_Value maxTarget = 0;    
    for(int time = 0; time < numTime - 1; time++)
    {
    	maxTarget = max(maxTarget, TARGET_VALUE(time));
    }    
    
    SYNTHESIS_VALUE = XORSHIFT_NEXT_VALUE * maxTarget;
    DECAY_VALUE = XORSHIFT_NEXT_VALUE * 1;
}
