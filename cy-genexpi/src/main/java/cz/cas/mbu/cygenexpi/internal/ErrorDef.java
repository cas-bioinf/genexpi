package cz.cas.mbu.cygenexpi.internal;

import org.cytoscape.work.Tunable;

import cz.cas.mbu.cygenexpi.RememberAllValues;

@RememberAllValues
public class ErrorDef {
	@Tunable(description="Absolute error term", groups={"Error"})
	public double absoluteError;

	@Tunable(description="Relative error term", groups={"Error"})
	public double relativeError;

	public ErrorDef(double absoluteError, double relativeError) {
		super();
		this.absoluteError = absoluteError;
		this.relativeError = relativeError;
	}
	
	public double getErrorMargin(double value)
	{
		return value * relativeError + absoluteError;
	}
}
