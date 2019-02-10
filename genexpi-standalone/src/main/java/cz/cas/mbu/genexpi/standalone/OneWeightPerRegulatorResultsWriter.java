package cz.cas.mbu.genexpi.standalone;

import java.util.List;

import cz.cas.mbu.genexpi.compute.OneWeightPerRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.InferenceResult;

public class OneWeightPerRegulatorResultsWriter extends AbstractResultsWriter<OneWeightPerRegulatorInferenceTask> {

	private final int numRegulators;
	
	
	public OneWeightPerRegulatorResultsWriter(int numRegulators) {
		super();
		this.numRegulators = numRegulators;
	}

	@Override
	protected void outputSpecificHeader(StringBuilder headerBuilder) {        
		headerBuilder.append("target");
        if(numRegulators == 1)
        {
        	headerBuilder.append(",reg");
        }
        else
        {
        	for(int i = 0; i < numRegulators; i++)
            {
        		headerBuilder.append(",reg").append(i + 1);
            }        
        }		
	}

	@Override
	protected void outputSpecificResult(StringBuilder lineBuilder, List<String> names, List<String> rawColumns,
			OneWeightPerRegulatorInferenceTask task, InferenceResult result) {
		
		if(task != null) {
	        lineBuilder.append(names.get(task.getTargetID()));
	        
	        for(int reg = 0; reg < numRegulators; reg++)
	        {
	        	lineBuilder.append(",").append(names.get(task.getRegulatorIDs()[reg]));            	
	        }
		} else {
    		lineBuilder.append(rawColumns.get(0));
            for(int reg = 0; reg < numRegulators; reg++)
            {
            	lineBuilder.append(",").append(rawColumns.get(reg + 1));            	
            }			
		}
	}

}
