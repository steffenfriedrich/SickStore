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
        super();
    }

    public ServerResponseException(long clientRequestID, Exception exception) {
        super(clientRequestID);
        this.exception = exception;
    }
}
