package database.messages.exception;

public class InsertException extends DatabaseException {

    private static final long serialVersionUID = 8026915664534414823L;

    public InsertException(String string) {
        super(string);
    }

    public InsertException() {
    }
}
