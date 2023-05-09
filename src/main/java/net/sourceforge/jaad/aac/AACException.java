package net.sourceforge.jaad.aac;

/**
 * Standard exception, thrown when decoding of an AAC frame fails.
 * The message gives more detailed information about the error.
 * @author in-somnia
 */
public class AACException extends RuntimeException {
	private static final long serialVersionUID = -3534371853313116173L;
	public AACException(String message) {
		super(message);
	}
	public AACException(String message, Throwable cause) {
		super(message, cause);
	}
	public AACException() {
	}
	public AACException(Throwable cause) {
		super(cause);
	}
	public AACException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
