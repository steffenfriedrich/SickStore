package database.messages;

public class ServerResponseDelete extends ServerResponse {

    private ServerResponseDelete() {
        super();
    }

    public ServerResponseDelete(long clientRequestID) {
        super(clientRequestID);
    }
}
