package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestDelete extends ClientWriteRequest {

    @SuppressWarnings("unused")
    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String table, String key) {
        super(table, key);
    }

    public ClientRequestDelete(String table, String key, String destinationServer) {
        super(table, key, destinationServer);
    }

    public ClientRequestDelete(String table, String key, WriteConcern writeConcern) {
        super(table, key, writeConcern);
    }

    public ClientRequestDelete(String table, String key, WriteConcern writeConcern, String destinationNode) {
        super(table, key, writeConcern, destinationNode);
    }
}
