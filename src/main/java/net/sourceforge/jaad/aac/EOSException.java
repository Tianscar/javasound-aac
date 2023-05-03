package net.sourceforge.jaad.aac;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 21.12.18
 * Time: 13:30
 */
public class EOSException extends AACException {

    public EOSException(String message) {
        super(message);
    }

    public EOSException(Throwable cause) {
        super(cause);
    }
}
