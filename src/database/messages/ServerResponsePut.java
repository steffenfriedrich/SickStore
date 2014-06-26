package database.messages;

public class ServerResponsePut extends ServerResponse {

    private ServerResponsePut() {
    }

    public ServerResponsePut(Long id) {
        super();
        this.id = id;
    }
}
