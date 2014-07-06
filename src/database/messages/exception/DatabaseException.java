package database.messages.exception;

public class DatabaseException extends Exception {

    private static final long serialVersionUID = 6872353557036876667L;

    private String message = null;

    public DatabaseException() {
        super();
    }

    public DatabaseException(String message) {
        this();
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}