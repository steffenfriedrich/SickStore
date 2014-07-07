package de.unihamburg.pimpstore.database.messages;

public class ServerResponseUpdate extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseUpdate() {
        super();
    }

    public ServerResponseUpdate(long clientRequestID) {
        super(clientRequestID);
    }
}
