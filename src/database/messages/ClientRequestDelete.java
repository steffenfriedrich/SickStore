package database.messages;

public class ClientRequestDelete extends ClientRequest {

    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String table, String key) {
        super(table, key);
    }
}
