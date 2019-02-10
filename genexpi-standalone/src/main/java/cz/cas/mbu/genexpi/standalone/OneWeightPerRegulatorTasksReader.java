package cz.cas.mbu.genexpi.standalone;

import java.util.Arrays;

import cz.cas.mbu.genexpi.compute.OneWeightPerRegulatorInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;

public class OneWeightPerRegulatorTasksReader extends AbstractTasksReader<OneWeightPerRegulatorInferenceTask>{

	public OneWeightPerRegulatorTasksReader(int numRegulators) {
		super(numRegulators + 1, numRegulators);
	}

	@Override
	protected OneWeightPerRegulatorInferenceTask createTask(int[] profileIDs, RegulationType[] regulationTypes) {
		int[] regulators = Arrays.copyOfRange(profileIDs, 1, profileIDs.length);
		int target = profileIDs[0];
		return new OneWeightPerRegulatorInferenceTask(regulators, target, regulationTypes);
	}

}
