package cz.cas.mbu.cygenexpi.internal;

import cz.cas.mbu.genexpi.compute.GeneProfile;
import cz.cas.mbu.genexpi.compute.IntegrateResults;

public class FitMetrics {
	public static double fitQuality(double[] expressionProfile, ErrorDef errorDef, double[] predictedProfile)
	{
		int numWithinErrorBounds = 0;
		for(int t = 0; t < predictedProfile.length; t++)
		{
			double measuredValue = expressionProfile[t];
			double errorMargin =  measuredValue * errorDef.relativeError + errorDef.absoluteError;
			if(Math.abs(measuredValue - predictedProfile[t]) < errorMargin)
			{
				numWithinErrorBounds++; 
			}
		}			
		
		double quality = (double)numWithinErrorBounds / (double)predictedProfile.length;
		return quality;
	}
	
	public static double fitQuality(GeneProfile<?> expressionProfile, ErrorDef errorDef, double[] predictedProfile)
	{
		int numWithinErrorBounds = 0;
		for(int t = 0; t < predictedProfile.length; t++)
		{
			double measuredValue = expressionProfile.getProfile().get(t).doubleValue();
			double errorMargin =  measuredValue * errorDef.relativeError + errorDef.absoluteError;
			if(Math.abs(measuredValue - predictedProfile[t]) < errorMargin)
			{
				numWithinErrorBounds++; 
			}
		}			
		
		double quality = (double)numWithinErrorBounds / (double)predictedProfile.length;
		return quality;
	}
	
}
