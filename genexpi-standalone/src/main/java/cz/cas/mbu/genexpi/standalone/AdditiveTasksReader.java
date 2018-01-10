package cz.cas.mbu.genexpi.standalone;

import java.util.Arrays;

import cz.cas.mbu.genexpi.compute.AdditiveRegulationInferenceTask;
import cz.cas.mbu.genexpi.compute.RegulationType;

public class AdditiveTasksReader extends AbstractTasksReader<AdditiveRegulationInferenceTask>{

	public AdditiveTasksReader(int numRegulators) {
		super(numRegulators + 1, numRegulators);
	}

	@Override
	protected AdditiveRegulationInferenceTask createTask(int[] profileIDs, RegulationType[] regulationTypes) {
		int[] regulators = Arrays.copyOfRange(profileIDs, 1, profileIDs.length);
		int target = profileIDs[0];
		return new AdditiveRegulationInferenceTask(regulators, target, regulationTypes);
	}

}
