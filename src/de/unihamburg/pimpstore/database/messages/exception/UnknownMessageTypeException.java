package de.unihamburg.pimpstore.database.messages.exception;

public class UnknownMessageTypeException extends DatabaseException {

    private static final long serialVersionUID = -6776306738124955527L;

    public UnknownMessageTypeException() {
    }

    public UnknownMessageTypeException(String string) {
        super(string);
    }
}
