package cz.cas.mbu.cygenexpi.internal;

import org.cytoscape.work.Tunable;

import cz.cas.mbu.cygenexpi.RememberAllValues;

@RememberAllValues
public class ErrorDef {
	@Tunable(description="Absolute error term", groups={"Error"})
	public double absoluteError;

	@Tunable(description="Relative error term", groups={"Error"})
	public double relativeError;

	@Tunable(description="Minimal error", groups={"Error"})
	public double minimalError;
	
	public static final ErrorDef DEFAULT = new ErrorDef(0, 0.2, 0);
	
	public ErrorDef(double absoluteError, double relativeError, double minimalError) {
		super();
		this.absoluteError = absoluteError;
		this.relativeError = relativeError;
		this.minimalError = minimalError;
	}
	
	public double getErrorMargin(double value)
	{
		return Math.max(value * relativeError + absoluteError, minimalError);
	}
}
