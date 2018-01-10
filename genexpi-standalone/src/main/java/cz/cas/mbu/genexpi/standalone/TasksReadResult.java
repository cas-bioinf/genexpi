package cz.cas.mbu.genexpi.standalone;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TasksReadResult<T> {
	private final List<T> inferenceTasks;
	
	/**
	 * keeps track of which tasks corresponds to lines in original file (useful when some tasks cannot be executed and are excluded from the batch).
	 * These data simply allow to output even tasks that were not translated to valid instances
	 */
	private final List<Integer> inferenceTasksForOriginalLines;  
    
	private final List<String> errors;
	
    private final List<List<String>> rawColumns;

	public TasksReadResult(List<T> inferenceTasks, List<Integer> inferenceTasksForOriginalLines, List<String> errors,
			List<List<String>> rawColumns) {
		super();
		this.inferenceTasks = inferenceTasks;
		this.inferenceTasksForOriginalLines = inferenceTasksForOriginalLines;
		this.errors = errors;
		this.rawColumns = rawColumns;
	}

	public List<T> getInferenceTasks() {
		return inferenceTasks;
	}

	public List<Integer> getInferenceTasksForOriginalLines() {
		return inferenceTasksForOriginalLines;
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<List<String>> getRawColumns() {
		return rawColumns;
	}
    
    
}
