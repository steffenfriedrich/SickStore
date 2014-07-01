package database.messages;

public class ServerResponseDelete extends ServerResponse {

    private ServerResponseDelete() {
    }

    public ServerResponseDelete(Long id) {
        super();
        this.id = id;
    }
}
