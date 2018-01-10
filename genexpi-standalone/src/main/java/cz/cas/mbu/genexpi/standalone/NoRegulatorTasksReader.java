package cz.cas.mbu.genexpi.standalone;

import cz.cas.mbu.genexpi.compute.NoRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;

public class NoRegulatorTasksReader extends AbstractTasksReader<NoRegulatorInferenceTask>{

	public NoRegulatorTasksReader() {
		super(1, 0);
	}

	@Override
	protected NoRegulatorInferenceTask createTask(int[] profileIDs, RegulationType[] regulationTypes) {
		return new NoRegulatorInferenceTask(profileIDs[0]);
	}

}
