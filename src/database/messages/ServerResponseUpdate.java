package database.messages;

public class ServerResponseUpdate extends ServerResponse {

    private ServerResponseUpdate() {
    }

    public ServerResponseUpdate(Long id) {
        super();
        this.id = id;
    }
}
