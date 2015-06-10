package de.unihamburg.sickstore.database.messages.exception;

public class NotConnectedException extends DatabaseException {

    private static final long serialVersionUID = -6547576334711813385L;

    public NotConnectedException() {
    }

    public NotConnectedException(String string) {
        super(string);
    }

}
