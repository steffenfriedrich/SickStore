package de.unihamburg.sickstore.database.messages;

public class ServerResponseDelete extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseDelete() {
        super();
    }

    public ServerResponseDelete(int clientRequestID) {
        super(clientRequestID);
    }

    @Override
    public String toString() {
        return "DELETE";
    }
}
