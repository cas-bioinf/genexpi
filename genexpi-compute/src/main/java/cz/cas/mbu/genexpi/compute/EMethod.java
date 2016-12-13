package cz.cas.mbu.genexpi.compute;

public enum EMethod {
	Annealing;
	
	public String getMethodSource()
	{
		return name() + ".cl";
	}
	
	public String getKernelBaseName()
	{
		return name();
	}
}
