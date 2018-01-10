package cz.cas.mbu.genexpi.compute;

import java.util.List;

public interface IInferenceEngine<NUMBER_TYPE extends Number, TASK_TYPE> {
	public InferenceModel getModel();
	public List<InferenceResult> compute(List<GeneProfile<NUMBER_TYPE>> geneProfiles, List<TASK_TYPE> inferenceTasks);
}
