package de.unihamburg.sickstore.database.messages.exception;

public class NoValueProvidedException extends DatabaseException {

    private static final long serialVersionUID = -6547576334711813385L;

    public NoValueProvidedException() {
    }

    public NoValueProvidedException(String string) {
        super(string);
    }

}
