package de.unihamburg.sickstore.database.messages;

public class ClientRequestDelete extends ClientRequest {

    @SuppressWarnings("unused")
    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String table, String key) {
        super(table, key);
    }
}
