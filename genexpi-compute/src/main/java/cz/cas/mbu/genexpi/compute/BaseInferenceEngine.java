/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cas.mbu.genexpi.compute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.LocalSize;
import com.nativelibs4java.util.IOUtils;

public abstract class BaseInferenceEngine<NUMBER_TYPE extends Number, TASK_TYPE> implements IInferenceEngine<NUMBER_TYPE, TASK_TYPE> {
    
    protected final Class<NUMBER_TYPE> elementClass;
    
    protected final CLContext context;
    protected final InferenceModel model;
    protected final EMethod method;
    protected final EErrorFunction errorFunction;
    protected final ELossFunction lossFunction;    
    
    protected final boolean useCustomTimeStep;
    protected final float customTimeStep;
    
    protected final CLProgram program;
    protected final CLKernel kernel;
    
    protected final boolean verbose;
    protected final int numIterations;
    protected final boolean preventFullOccupation;

    public BaseInferenceEngine(Class<NUMBER_TYPE> elementClass, CLContext context, InferenceModel model, EMethod method,
			EErrorFunction errorFunction, ELossFunction lossFunction, boolean useCustomTimeStep, Float customTimeStep, boolean verbose,
			int numIterations, boolean preventFullOccupation) throws IOException {
		super();
		
		if(context == null) {
			throw new NullPointerException("You have to provide context");
		}
		if(model == null) {
			throw new NullPointerException("You have to provide inference model");
		}
		if(method == null) {
			throw new NullPointerException("You have to provide method");
		}
		if(lossFunction == null) {
			throw new NullPointerException("You have to provide loss function");
		}
		
		this.elementClass = elementClass;
		this.context = context;
		this.model = model;
		this.method = method;
		this.errorFunction = errorFunction;
		this.lossFunction = lossFunction;
		this.verbose = verbose;
		this.numIterations = numIterations;
		this.preventFullOccupation = preventFullOccupation;
		
		this.useCustomTimeStep = useCustomTimeStep;
		if(useCustomTimeStep)
		{
			this.customTimeStep = customTimeStep;
		}
		else
		{
			this.customTimeStep = Float.NaN;
		}

		String kernelName = model.getKernelName(method.getKernelBaseName());
		
		
		StringBuilder sourceBuilder = new StringBuilder();

		
        if(errorFunction != null)
        {
        	sourceBuilder.append("#define ").append(errorFunction.getMacro()).append("\n");
        }
    	sourceBuilder.append("#define ").append(lossFunction.getMacro()).append("\n");
    	if(elementClass == Double.class) {
        	sourceBuilder.append("#define USE_DOUBLE 2\n"); //see CTSW_ON in Definitions.clh    		
    	}
    	else if(elementClass == Float.class) {
        	sourceBuilder.append("#define USE_DOUBLE 1\n"); //see CTSW_OFF in Definitions.clh   		
    	} else {
    		throw new IllegalArgumentException("Unsupported elementClass: " + elementClass);
    	}
		
        sourceBuilder.append(IOUtils.readText(BaseInferenceEngine.class.getResource("Definitions.clh")));
        sourceBuilder.append(IOUtils.readText(BaseInferenceEngine.class.getResource("Utils.cl")));
        sourceBuilder.append(IOUtils.readText(BaseInferenceEngine.class.getResource("XorShift1024.cl")));
        for(String sourceFile : model.getModelSources()) {
        	sourceBuilder.append(IOUtils.readText(BaseInferenceEngine.class.getResource(sourceFile)));
        }
        sourceBuilder.append(IOUtils.readText(BaseInferenceEngine.class.getResource(method.getMethodSource())));
        
        String combinedSource = sourceBuilder.toString(); 

        //Commented out block of debug code. Slightly ashamed of this.
//        File srcOut = new File(kernelName + ".cl");
//        try (Writer w = new FileWriter(srcOut))
//		{
//        	w.write(combinedSource);
//		}
        
        program = context.createProgram(combinedSource);
        
        if(useCustomTimeStep)
        {
        	program.defineMacro("CUSTOM_TIME_STEP", Float.toString(customTimeStep));
        }
               
        if(model.getAdditionalDefines() != null)
        {
        	for(int define = 0; define < model.getAdditionalDefines().length; define++)
        	{
    			program.defineMacro(model.getAdditionalDefines()[define][0], model.getAdditionalDefines()[define][1]);
        	}
        }
        
        
        kernel = program.createKernel(kernelName);		
        
	}
   
    
    
    public boolean isVerbose() {
		return verbose;
	}

	protected CLEvent[] executeKernel(CLQueue queue, int numItems)
    {
        
        int maxComputeUnits = queue.getDevice().getMaxComputeUnits();
        
        int maxItemsPerExecution ;
        if (preventFullOccupation) {
        	maxItemsPerExecution = Math.max(maxComputeUnits - 1, 1);
        }
        else
        {
            maxItemsPerExecution = 128;        	
        }
        
        List<CLEvent> eventsToWaitFor = new ArrayList<>();
        for(int offset = 0; offset < numItems; offset += maxItemsPerExecution )
        {
            int itemsLeft = numItems - offset;
            int itemsForExecution;
            if(itemsLeft > maxItemsPerExecution)
            {
                itemsForExecution = maxItemsPerExecution;
            }
            else
            {
                itemsForExecution = itemsLeft;
            }
            
            long[] globalOffset = new long[] {offset, 0};
            long[] globalWorkSize = new long[] { itemsForExecution, numIterations };
            long[] localWorkSize = new long[] {1, numIterations};
            CLEvent computationFinishedEvent = kernel.enqueueNDRange(queue, globalOffset, globalWorkSize, localWorkSize);
            
            if(preventFullOccupation)
            {
            	computationFinishedEvent.waitFor();
            }
            else
            {
            	eventsToWaitFor.add(computationFinishedEvent);
            }
        }
      
        CLEvent[] eventsToWaitForArray = new CLEvent[eventsToWaitFor.size()];
        eventsToWaitFor.toArray(eventsToWaitForArray);
                        
        return eventsToWaitForArray; 
    }
    
	protected LocalSize getLocalSizeByElementClass(long numElements) {
		if(elementClass == Double.class) {
			return LocalSize.ofDoubleArray(numElements);
		} else if(elementClass == Float.class) {
			return LocalSize.ofFloatArray(numElements);
		}
		throw new IllegalArgumentException("Unrecognized elementClass: " + elementClass);
	}
	
    protected long prepareXorShiftParameters(List<Object> argumentList, int numItems)
    {
        ByteOrder byteOrder = context.getByteOrder();
        Pointer<Long> xorShiftStatesPtr = Pointer.allocateLongs(numItems * numIterations * 16).order(byteOrder);
        XorShift1024 xorShiftBase = new XorShift1024();
        xorShiftBase.InitFromSecureRandomAndSplitMix();
        
        for(int inferenceUnit = 0; inferenceUnit < numItems; inferenceUnit++)
        {
            for(int iter = 0; iter < numIterations; iter++)
            {
                xorShiftBase.Jump();
                for(int index = 0; index < 16; index++)
                {
                    xorShiftStatesPtr.set((index * numItems + inferenceUnit) * numIterations + iter, xorShiftBase.GetState()[index]);
                }                            
            }
        }
        
        CLBuffer<Long> xorShiftStates = context.createLongBuffer(CLMem.Usage.InputOutput, xorShiftStatesPtr);
        argumentList.add(xorShiftStates);
        
        CLBuffer<Integer> xorShiftCounters = context.createIntBuffer(CLMem.Usage.InputOutput, numItems * numIterations);
        argumentList.add(xorShiftCounters);
        return xorShiftStates.getByteCount() + xorShiftCounters.getByteCount();
    }
    
    protected long prepareBaseParameters(List<Object> argumentList, List<GeneProfile<NUMBER_TYPE>> genes, int[] targetIDs)
    {
        ByteOrder byteOrder = context.getByteOrder();
        int numTimePoints = genes.get(0).getProfile().size();
        
        Pointer<NUMBER_TYPE> profilesPtr = Pointer.allocateArray(elementClass, numTimePoints * genes.size()).order(byteOrder);

        boolean warnedOfWrongValues = false;
		for (int regulator = 0; regulator < genes.size(); regulator++) 
		{
			List<NUMBER_TYPE> regulatorProfile = genes.get(regulator).getProfile();
			for (int timePoint = 0; timePoint < numTimePoints; timePoint++) 
			{
				NUMBER_TYPE regulatorValue = regulatorProfile.get(timePoint);
				profilesPtr.set(timePoint * genes.size() + regulator, regulatorValue);
				if(regulatorValue.doubleValue() < 0 || Double.isInfinite(regulatorValue.doubleValue()) || Double.isNaN(regulatorValue.doubleValue()))
				{
					if(!warnedOfWrongValues)
					{
						System.out.println("Warning: some gene values are negative, infinity or NaN (e.g. for gene `" + genes.get(regulator).getName() + "` and time " + timePoint + ")");						
						warnedOfWrongValues = true;
					}
				}
			}
		}
        long totalBytes = 0;
        
        CLBuffer<NUMBER_TYPE> profiles = context.createBuffer(CLMem.Usage.Input, profilesPtr);
        totalBytes += profiles.getByteCount(); 
        
        argumentList.add(profiles);
        
        totalBytes += prepareProfileIDParameter(argumentList, targetIDs);
               
        argumentList.add(getLocalSizeByElementClass(numTimePoints)); //local target copy
        
        argumentList.add(genes.size());
        
        argumentList.add(numTimePoints);
                
        return totalBytes;    	
    }
    
    
    protected long prepareProfileIDParameter(List<Object> argumentList, int[] profileIDs)
    {
        ByteOrder byteOrder = context.getByteOrder();
        int numItems = profileIDs.length;
        
        Pointer<Integer> indicesPtr = Pointer.allocateInts(numItems).order(byteOrder);
        indicesPtr.setInts(profileIDs);
        
        CLBuffer<Integer> indices = context.createIntBuffer(CLMem.Usage.Input, indicesPtr);
        
        argumentList.add(indices);
        
        return indices.getByteCount();
    	
    }    
    
    protected long prepareWeightConstraintsParameter(List<Object> argumentList, int[] weightConstraints)
    {
    	if(weightConstraints == null)
    	{
    		argumentList.add(null);
    		return 0; 
    	}
    	else
    	{
            ByteOrder byteOrder = context.getByteOrder();
            int numItems = weightConstraints.length;
            
            Pointer<Integer> constraintsPtr = Pointer.allocateInts(numItems).order(byteOrder);
            constraintsPtr.setInts(weightConstraints);
            
            CLBuffer<Integer> constraints = context.createIntBuffer(CLMem.Usage.Input, constraintsPtr);
            
            argumentList.add(constraints);
                		
            return constraints.getByteCount();
    	}
    }
    
    protected OutputPointers prepareOutputParameters(List<Object> argumentList, int numItems)
    {
        CLBuffer<NUMBER_TYPE> optimizedParams = context.createBuffer(CLMem.Usage.InputOutput, elementClass, numItems * numIterations * model.getNumParams());
        CLBuffer<NUMBER_TYPE> errors = context.createBuffer(CLMem.Usage.InputOutput, elementClass, numItems * numIterations);
        argumentList.add(optimizedParams);
        argumentList.add(errors);
        return new OutputPointers(optimizedParams, errors);
    }
    
   
    protected List<InferenceResult> gatherInferenceResults(CLQueue queue, OutputPointers pointers, CLEvent[] eventsToWaitFor, int numItems)
    {
    	try {
	        Pointer<NUMBER_TYPE> optimizedParamsPtr = pointers.getOptimizedParams().read(queue, eventsToWaitFor); // blocks until the kernel finished
	        Pointer<NUMBER_TYPE> errorPtr = pointers.getErrors().read(queue, eventsToWaitFor); // blocks until the kernel finished
	    
	        List<InferenceResult> results = new ArrayList<>(numItems);
	        int nanCount = 0;
	        for(int item = 0; item < numItems; item++)
	        {
	            double minError = Double.POSITIVE_INFINITY;
	            int minErrorIndex = -1;
	            for(int iteration = 0; iteration < numIterations; iteration++)
	            {
	                NUMBER_TYPE errorVal = errorPtr.get(item * numIterations+ iteration);
	                
	                if(Double.isNaN(errorVal.doubleValue()))
	                {
	                	nanCount++;
	                }
	                
	                if(errorVal.doubleValue() < minError)
	                {
	                    minError = errorVal.doubleValue();
	                    minErrorIndex = iteration;
	                }
	            }
	            
	            double[] bestParams = new double[model.getNumParams()];
	                        
	            for(int param = 0; param < model.getNumParams(); param++)            
	            {
	            	if(minErrorIndex < 0)
	            	{
	            		bestParams[param] = Double.NaN;            		
	            	}
	            	else
	            	{
	            		bestParams[param] = optimizedParamsPtr.get((param * numItems + item) * numIterations + minErrorIndex).doubleValue();
	            	}
	            }            
	                
	            results.add(new InferenceResult(bestParams, minError));                
	        }  
	        
	        if(nanCount > 0) 
	        {
	            System.out.println("Encountered " + nanCount + " NaNs out of " + (numItems * numIterations) + " runs.");        
	        }
	                
	        return results;
    	} catch(CLException.OutOfResources outOfResourcesEx)
    	{
    		boolean isGpu = Arrays.stream(context.getDevices()).anyMatch(device -> device.getType().contains(CLDevice.Type.GPU));    		
    		
    		if(isGpu && !preventFullOccupation)
    		{
    			throw new SuspectGPUResetByOSException("The computation was not succesful, a possible cause is a reset by the OS.\n"
    					+ "If your are running computations on the same GPU the runs your main display, consider preventing full occupation of the GPU or running on a CPU instead.", outOfResourcesEx);
    		}    		
    		else
    		{
    			throw outOfResourcesEx;
    		}
    	}
    }
    
    protected int regulationTypeToInt(RegulationType w)
    {
    	switch (w) {
	    	case All : return 0;
	    	case NegativeOnly: return -1;
	    	case PositiveOnly: return 1;
	    	default: {
	    		throw new IllegalStateException("Unrecognized regulation type: " + w);
	    	}
    	}
    	
    }
    

    

    protected class OutputPointers
    {
    	CLBuffer<NUMBER_TYPE> optimizedParams;
        CLBuffer<NUMBER_TYPE> errors;
		
        public OutputPointers(CLBuffer<NUMBER_TYPE> optimizedParams, CLBuffer<NUMBER_TYPE> errors) {
			super();
			this.optimizedParams = optimizedParams;
			this.errors = errors;
		}

		public CLBuffer<NUMBER_TYPE> getOptimizedParams() {
			return optimizedParams;
		}

		public CLBuffer<NUMBER_TYPE> getErrors() {
			return errors;
		}
        
        public long getByteCount()
        {
        	return optimizedParams.getByteCount() + errors.getByteCount();    	        	
        }
     

    }

    @Override
	public InferenceModel getModel() {
		return model;
	}

	public EMethod getMethod() {
		return method;
	}

	public EErrorFunction getErrorFunction() {
		return errorFunction;
	}

	public ELossFunction getLossFunction() {
		return lossFunction;
	}

	public boolean isUseCustomTimeStep() {
		return useCustomTimeStep;
	}

	public float getCustomTimeStep() {
		return customTimeStep;
	}
    
    
	protected CLQueue createQueue() {
		CLQueue queue;
        
        try {
            queue = context.createDefaultOutOfOrderQueue();        	
        }
        catch(CLException ex)
        {
        	if(verbose) {
        		System.out.println("Could not create out-of-order queue. Using default queue.");
        	}
            queue = context.createDefaultQueue();        	
        }
		return queue;
	}
	
}