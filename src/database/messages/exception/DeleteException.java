package database.messages.exception;

public class DeleteException extends DatabaseException {

    private static final long serialVersionUID = 8026915664534414823L;

    public DeleteException() {
    }

    public DeleteException(String string) {
        super(string);
    }

}
