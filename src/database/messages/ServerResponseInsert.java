package database.messages;

public class ServerResponseInsert extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseInsert() {
        super();
    }

    public ServerResponseInsert(long clientRequestID) {
        super(clientRequestID);
    }

}
