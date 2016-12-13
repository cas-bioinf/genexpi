package cz.cas.mbu.cygenexpi;

public class GNException extends RuntimeException{

	public GNException(String message, Throwable cause) {
		super(message, cause);
	}

	public GNException(String message) {
		super(message);
	}

	public GNException(Throwable cause) {
		super(cause);
	}

}
