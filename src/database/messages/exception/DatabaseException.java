package database.messages.exception;

public class DatabaseException extends Exception {

    private static final long serialVersionUID = 6872353557036876667L;

    private String message = null;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DatabaseException() {
        super();
    }

    public DatabaseException(String message) {
        this();
        this.message = message;
    }
}