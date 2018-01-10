package cz.cas.mbu.genexpi.standalone;

import cz.cas.mbu.genexpi.compute.CooperativeRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;

public class CooperativeTasksReader extends AbstractTasksReader<CooperativeRegulationInferenceTask>{

	public CooperativeTasksReader() {
		super(3, 1);
	}

	@Override
	protected CooperativeRegulationInferenceTask createTask(int[] profileIDs, RegulationType[] regulationTypes) {
		return new CooperativeRegulationInferenceTask(profileIDs[1], profileIDs[2], profileIDs[0], regulationTypes[0]);
	}

}
