package de.unihamburg.sickstore.database.messages.exception;

public class WriteForbiddenException extends DatabaseException {

    private static final long serialVersionUID = 2198505941320294238L;

    public WriteForbiddenException(String message) {
        super(message);
    }
}
