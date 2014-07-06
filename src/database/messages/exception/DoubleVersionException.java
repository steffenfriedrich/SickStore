package database.messages.exception;

/**
 * This exception is thrown, when the exact same timestamp is used to store two
 * version objects under the same key.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class DoubleVersionException extends DatabaseException {

    private static final long serialVersionUID = 8026915664534414823L;

    public DoubleVersionException() {
    }

    public DoubleVersionException(String string) {
        super(string);
    }

}
