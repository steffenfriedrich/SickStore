package database.messages;

import database.messages.exception.DatabaseException;

public class ServerResponseException extends ServerResponse {

    private String message;
    private String className;

    @SuppressWarnings("unused")
    private ServerResponseException() {
        super();
    }

    public ServerResponseException(long clientRequestID, Exception e) {
        super(clientRequestID);
        this.message = e.getMessage();
        this.className = e.getClass().getCanonicalName();
    }

    /**
     * 
     * @return a database exception
     */
    public Exception getException() {
        Object object = null;
        try {
            Class<?> c = null;
            c = Class.forName(className);
            object = c.newInstance();
            ((DatabaseException) object).setMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (object instanceof DatabaseException) {
            return (DatabaseException) object;
        } else {
            return new Exception(message);
        }
    }

}
