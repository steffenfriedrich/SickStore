package de.unihamburg.pimpstore.database.messages;

public class ClientRequestDelete extends ClientRequest {

    @SuppressWarnings("unused")
    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String table, String key) {
        super(table, key);
    }
}
