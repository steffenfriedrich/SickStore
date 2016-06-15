package de.unihamburg.sickstore.database.messages.exception;

public class NoColumnProvidedException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    public NoColumnProvidedException() {
    }

    public NoColumnProvidedException(String string) {
        super(string);
    }
}
