package de.unihamburg.sickstore.database.messages;

public class ServerResponseUpdate extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseUpdate() {
        super();
    }

    public ServerResponseUpdate(int clientRequestID) {
        super(clientRequestID);
    }

    @Override
    public String toString() {
        return "UPDATE";
    }
}
