package database.messages;

public class ServerResponseInsert extends ServerResponse {

    private ServerResponseInsert() {
        super();
    }

    public ServerResponseInsert(long clientRequestID) {
        super(clientRequestID);
    }

}
