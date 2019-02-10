package cz.cas.mbu.genexpi.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import cz.cas.mbu.genexpi.compute.IInferenceEngine;
import cz.cas.mbu.genexpi.compute.InferenceResult;

public abstract class AbstractResultsWriter<TASK_TYPE> {
	
	protected abstract void outputSpecificHeader(StringBuilder headerBuilder);
	protected abstract void outputSpecificResult(StringBuilder lineBuilder, List<String> names, List<String> rawColumns, TASK_TYPE task, InferenceResult result);
	
	public void write(List<String> names, TasksReadResult<TASK_TYPE> readResult, IInferenceEngine<?, TASK_TYPE> compute, List<InferenceResult> results, File outputFile) throws IOException {
        List<String> outCSVMain = new ArrayList<>();
        
        StringBuilder headerBuilder = new StringBuilder();
        outputSpecificHeader(headerBuilder);
        
        for(int i = 0; i < compute.getModel().getNumParams(); i++)
        {
        	headerBuilder.append(",").append(compute.getModel().getParameterNames()[i]);        	
        }
        headerBuilder.append(",error");
        outCSVMain.add(headerBuilder.toString());
        
        List<Integer> inferenceTasksForOriginalLines = readResult.getInferenceTasksForOriginalLines();
        List<TASK_TYPE> inferenceTasks = readResult.getInferenceTasks();
        List<List<String>> rawColumns = readResult.getRawColumns();
        
        for(int i = 0; i < inferenceTasksForOriginalLines.size(); i++){
            StringBuilder lineBuilder = new StringBuilder();
            
        	if(inferenceTasksForOriginalLines.get(i) != null)
        	{
        		int taskIndex = inferenceTasksForOriginalLines.get(i);
	            TASK_TYPE item = inferenceTasks.get(taskIndex);
	            InferenceResult result = results.get(taskIndex);
	            
	            outputSpecificResult(lineBuilder, names, rawColumns.get(i), item, result);
	            	            
	            for(int p = 0; p < compute.getModel().getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append(result.getParameters()[p]);        	
	            }            
	            lineBuilder.append(",").append(result.getError());
        	}
        	else
        	{
	            outputSpecificResult(lineBuilder, names, rawColumns.get(i), null, null);
	            
	            for(int p = 0; p < compute.getModel().getNumParams(); p++)
	            {
	            	lineBuilder.append(",").append("NaN");        	
	            }            
	            lineBuilder.append(",").append("NaN");        		
        	}
            outCSVMain.add(lineBuilder.toString());
        }
                
        Files.write(outputFile.toPath(), outCSVMain, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);		
	}
}
