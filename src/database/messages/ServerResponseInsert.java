package database.messages;

public class ServerResponseInsert extends ServerResponse {

    private ServerResponseInsert() {
    }

    public ServerResponseInsert(Long id) {
        super();
        this.id = id;
    }
}
