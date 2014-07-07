package de.unihamburg.pimpstore.database.messages;

public class ServerResponseDelete extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseDelete() {
        super();
    }

    public ServerResponseDelete(long clientRequestID) {
        super(clientRequestID);
    }
}
