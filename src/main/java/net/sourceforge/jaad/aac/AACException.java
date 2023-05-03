package net.sourceforge.jaad.aac;

/**
 * Standard exception, thrown when decoding of an AAC frame fails.
 * The message gives more detailed information about the error.
 * @author in-somnia
 */
public class AACException extends RuntimeException {

	public AACException(String message) {
		super(message);
	}

	public AACException(Throwable cause) {
		super(cause);
	}
}
