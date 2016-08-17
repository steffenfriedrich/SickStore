package de.unihamburg.sickstore.database.messages;

public class ServerResponseInsert extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseInsert() {
        super();
    }

    public ServerResponseInsert(int clientRequestID) {
        super(clientRequestID);
    }


    @Override
    public String toString() {
        return "INSERT";
    }
}
