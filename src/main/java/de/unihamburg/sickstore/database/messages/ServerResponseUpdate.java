package de.unihamburg.sickstore.database.messages;

public class ServerResponseUpdate extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseUpdate() {
        super();
    }

    public ServerResponseUpdate(long clientRequestID) {
        super(clientRequestID);
    }
}
