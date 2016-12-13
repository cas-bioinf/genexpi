/*
XorShift1024* as in http://xorshift.di.unimi.it/xorshift1024star.c
The state must be seeded so that it is not everywhere zero. If you have
a 64-bit seed, we suggest to seed a splitmix64 generator and use its
output to fill s. 
 */

#define XORSHIFT_STATE(index) (xorShiftStates[((index) * numTasks + taskID) * numIterations + iterationID])
#define XORSHIFT_COUNTER (xorShiftCounters[taskID * numIterations + iterationID])

#define XORSHIFT_PARAMS_DEF global ulong* xorShiftStates, global int* xorShiftCounters
#define XORSHIFT_PARAMS_PASS xorShiftStates, xorShiftCounters

unsigned long XorShiftNext(XORSHIFT_PARAMS_DEF, const int numTasks, const int numIterations)
{
    GET_IDS;
 
    int p = XORSHIFT_COUNTER;
    const ulong s0 = XORSHIFT_STATE(p);
    p = (p + 1) & 15;
    XORSHIFT_COUNTER = p; //doing memory ops as soon as possible
    ulong s1 = XORSHIFT_STATE(p);
    s1 ^= s1 << 31; // a
    ulong s_new = s1 ^ s0 ^ (s1 >> 11) ^ (s0 >> 30); // b,c
    XORSHIFT_STATE(p) = s_new;
    return s_new * 1181783497276652981UL;    
    
    //The original C code for reference and checks
    /*
        const uint64_t s0 = s[p];
	uint64_t s1 = s[p = (p + 1) & 15];
	s1 ^= s1 << 31; // a
	s[p] = s1 ^ s0 ^ (s1 >> 11) ^ (s0 >> 30); // b,c
	return s[p] * UINT64_C(1181783497276652981);     * */
}

void XorShiftInit(global int* xorShiftCounters, const int numTasks, const int numIterations)
{
    GET_IDS;
    
    XORSHIFT_COUNTER = 0;
}

double XorShiftNextDouble(XORSHIFT_PARAMS_DEF, const int numTasks, const int numIterations)
{
    ulong x = XorShiftNext(xorShiftStates, xorShiftCounters, numTasks, numIterations);
    x = 0x3FFUL << 52 | x >> 12;
    double d = *((double *)&x) - 1.0;
    return d;
}


float XorShiftNextFloat(XORSHIFT_PARAMS_DEF, const int numTasks, const int numIterations)
{
    //TODO a better way to get float
    return (float)XorShiftNextDouble(xorShiftStates, xorShiftCounters, numTasks, numIterations);
}

#if CTSW(USE_DOUBLE)
    #define XORSHIFT_NEXT_VALUE (XorShiftNextDouble(XORSHIFT_PARAMS_PASS, numTasks, numIterations))
#else
    #define XORSHIFT_NEXT_VALUE (XorShiftNextFloat(XORSHIFT_PARAMS_PASS, numTasks, numIterations))
#endif


T_Value XorShiftRandN(XORSHIFT_PARAMS_DEF, const int numTasks, const int numIterations)
{
    T_Value value = 0;
    for(int i = 0; i < 12 ; i++)
    {
        value += XORSHIFT_NEXT_VALUE;
    }
    return (value - 6);
}

#define XORSHIFT_RANDN (XorShiftRandN(XORSHIFT_PARAMS_PASS, numTasks, numIterations))
