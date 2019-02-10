package cz.cas.mbu.genexpi.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import cz.cas.mbu.genexpi.compute.RegulationType;

public abstract class AbstractTasksReader<T> {
	private final int expectedTasksColumns;
	private final int expectedConstraintsColumns;
			
	public AbstractTasksReader(int expectedTasksColumns, int expectedConstraintsColumns) {
		super();
		this.expectedTasksColumns = expectedTasksColumns;
		this.expectedConstraintsColumns = expectedConstraintsColumns;
	}

	protected abstract T createTask(int[] profileIDs, RegulationType[] regulationTypes);
	
	
	TasksReadResult<T> readTasks(List<String> names, File tasksFile, File constraintsFile) throws IOException {
        List<T> inferenceTasks = new ArrayList<>();
        List<Integer> inferenceTasksForOriginalLines = new ArrayList<>(); //keeps track of which tasks corresponds to lines in original file (useful when some tasks cannot be executed and are excluded from the batch).
        //These data simply allow to output even tasks that were not translated to valid instances 
        List<String> errors = new ArrayList<>();
        List<List<String>> rawColumns = new ArrayList<>();
                
        List<String> inputLines = Files.readAllLines(tasksFile.toPath());
        List<String> constraintLines = null;
        if(constraintsFile != null)
        {
        	constraintLines = Files.readAllLines(constraintsFile.toPath());
        	if(constraintLines.size() != inputLines.size())
        	{
        		throw new IllegalArgumentException("Number of tasks and constraints does not match");
        	}
        }
        for(int line = 0; line < inputLines.size(); line++)
        {
            String[] fields = inputLines.get(line).split(",");            
            int[] taskProfileIDs = new int[expectedTasksColumns];
            
            if(fields.length != expectedTasksColumns)
            {
            	throw new IllegalArgumentException("Number of profiles at tasks line " + (line + 1) + " (" + fields.length + ") does not match the expected number (" + expectedTasksColumns + ")");
            }
            
            String[] weightConstraintsFields = null; 
            RegulationType[] regulationTypes = new RegulationType[expectedConstraintsColumns];
            if(constraintsFile != null)
            {
            	weightConstraintsFields = constraintLines.get(line).split(",");
                regulationTypes = new RegulationType[expectedConstraintsColumns];
                if(weightConstraintsFields.length != expectedConstraintsColumns)
                {
                	throw new IllegalArgumentException("Number of constraints at line " + (line + 1) + " (" + weightConstraintsFields.length + ") does not match the expected number of constraints (" + expectedConstraintsColumns + ")");
                }
            } else {
            	for(int constraintIndex = 0; constraintIndex < expectedConstraintsColumns; constraintIndex++) {
            		regulationTypes[constraintIndex] = RegulationType.All;
            	}
            }
            
            StringBuilder errorBuilder = new StringBuilder();
            
            List<String> rawColumnsThisTask = new ArrayList<>();
            
            boolean columnsOK = true;
            for(int column = 0; column < expectedTasksColumns; column++)
            {
            	rawColumnsThisTask.add(fields[column]);
	            int profileIndex = names.indexOf(fields[column]);
	            if(profileIndex < 0)
	            {
	            	String error = "Didn't find profile " + fields[1] + " in the names file. ";
	            	errorBuilder.append(error);
	                columnsOK = false;
	            }
	            taskProfileIDs[column] = profileIndex;	            
            }
            if(constraintsFile != null)
            {
            	for(int constraintsColumn = 0; constraintsColumn < expectedConstraintsColumns; constraintsColumn++){
	            	try {
	            		int intRegulationType = Integer.parseInt(weightConstraintsFields[constraintsColumn]);
	            		if(intRegulationType == 1) 
	            		{
	            			regulationTypes[constraintsColumn] = RegulationType.PositiveOnly;
	            		}
	            		else if(intRegulationType == -1)
	            		{
	            			regulationTypes[constraintsColumn] = RegulationType.NegativeOnly;
	            		}
	            		else if(intRegulationType == 0)
	            		{
	            			regulationTypes[constraintsColumn] = RegulationType.All;
	            		}
	            		else
		            	{
		            		throw new IllegalArgumentException("Constraint at line " + (line + 1) + " column " + (constraintsColumn + 1) + " is not +-1 neither 0.");	            		
		            	}
	            	} catch (NumberFormatException nfe)
	            	{
	            		throw new IllegalArgumentException("Constraint at line " + (line + 1) + " column " + (constraintsColumn + 1) + " cannot be parsed as a number");
	            	}
            	}
            } else {
            	
            }
            
            
            rawColumns.add(rawColumnsThisTask);
                       
            if(columnsOK)
            {
                inferenceTasksForOriginalLines.add(inferenceTasks.size());

                inferenceTasks.add(createTask(taskProfileIDs, regulationTypes));
            }
            else
            {
                inferenceTasksForOriginalLines.add(null);            	
            }
            
            if(errorBuilder.length() > 0)
            {
            	errors.add(errorBuilder.toString());
                System.out.println("Task " + (line + 1) + ": " + errorBuilder.toString());
            }
            else
            {
            	errors.add(null);
            }
        }		
        return new TasksReadResult<>(inferenceTasks, inferenceTasksForOriginalLines, errors, rawColumns);
	}
}
