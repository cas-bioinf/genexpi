package cz.cas.mbu.genexpi.compute;

public class BaseDerivativeInferenceTask {

	protected final int[] regulatorIDs;
	protected final int targetID;

	public BaseDerivativeInferenceTask(int[] regulatorIDs, int targetID) {
		super();
		this.regulatorIDs = regulatorIDs;
		this.targetID = targetID;
	}

	public int getTargetID() {
		return targetID;
	}

	public int[] getRegulatorIDs() {
		return regulatorIDs;
	}

}