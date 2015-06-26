package de.unihamburg.sickstore.database.messages;

public class ClientRequestDelete extends ClientRequest {

    @SuppressWarnings("unused")
    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String destinationServer, String table, String key) {
        super(destinationServer, table, key);
    }
}
