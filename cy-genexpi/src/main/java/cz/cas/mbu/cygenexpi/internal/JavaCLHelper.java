package cz.cas.mbu.cygenexpi.internal;

public class JavaCLHelper {
	
	/**
	 * Storing and setting the classloader is a temporary workaround for https://github.com/nativelibs4java/nativelibs4java/issues/574
	 * @param r
	 */
	public static void runWithCLClassloader(Runnable r)
	{
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(JavaCLHelper.class.getClassLoader());
			r.run();
		}
		finally 
		{
			Thread.currentThread().setContextClassLoader(oldClassLoader);			
		}
		
	}
}
