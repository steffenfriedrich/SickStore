package database.messages;

public class ClientRequestGet extends ClientRequest {

    private ClientRequestGet() {
    }

    public ClientRequestGet(String key) {
        super();
        this.key = key;
    }
}
