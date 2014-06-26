package database.messages;

public class ServerResponseException extends ServerResponse {
    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    private Exception exception;

    private ServerResponseException() {
    }

    public ServerResponseException(Long id, Exception exception) {
        super();
        this.id = id;
        this.exception = exception;
    }

    public ServerResponseException(Exception exception) {
        this(-1l, exception);
    }
}
