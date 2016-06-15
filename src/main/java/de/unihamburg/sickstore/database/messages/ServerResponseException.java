package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.messages.exception.DatabaseException;

public class ServerResponseException extends ServerResponse {

    private String className;
    private String message;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (object instanceof DatabaseException) {
            ((DatabaseException) object).setMessage(message);
            return (DatabaseException) object;
        } else {
            return new Exception(message);
        }
    }

    @Override
    public String toString() {
        return "Exception";
    }
}
