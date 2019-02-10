package cz.cas.mbu.genexpi.standalone;

import java.util.List;

import cz.cas.mbu.genexpi.compute.CooperativeRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.InferenceResult;

public class CooperativeResultsWriter extends AbstractResultsWriter<CooperativeRegulationInferenceTask> {

	@Override
	protected void outputSpecificHeader(StringBuilder headerBuilder) {        
		headerBuilder.append("target,reg0,reg1");
	}

	@Override
	protected void outputSpecificResult(StringBuilder lineBuilder, List<String> names, List<String> rawColumns,
			CooperativeRegulationInferenceTask task, InferenceResult result) {
		
		if(task != null) {
	        lineBuilder.append(names.get(task.getTargetID()));
	        
	        for(int reg = 0; reg < 2; reg++)
	        {
	        	lineBuilder.append(",").append(names.get(task.getRegulatorIDs()[reg]));            	
	        }
		} else {
    		lineBuilder.append(rawColumns.get(0));
            for(int reg = 0; reg < 2; reg++)
            {
            	lineBuilder.append(",").append(rawColumns.get(reg + 1));            	
            }			
		}
	}

}
