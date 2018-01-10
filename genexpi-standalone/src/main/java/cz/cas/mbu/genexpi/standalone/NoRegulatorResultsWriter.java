package cz.cas.mbu.genexpi.standalone;

import java.util.List;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.InferenceResult;
import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;

public class NoRegulatorResultsWriter extends AbstractResultsWriter<NoRegulatorInferenceTask> {

	@Override
	protected void outputSpecificHeader(StringBuilder headerBuilder) {
		headerBuilder.append("target");
	}

	@Override
	protected void outputSpecificResult(StringBuilder lineBuilder, List<String> names, List<String> rawColumns,
			NoRegulatorInferenceTask task, InferenceResult result) {
		
		if(task != null) {
	        lineBuilder.append(names.get(task.getTargetID()));	        
		} else {
    		lineBuilder.append(rawColumns.get(0));
		}
	}

}
