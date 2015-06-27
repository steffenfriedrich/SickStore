package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestDelete extends ClientWriteRequest {

    @SuppressWarnings("unused")
    private ClientRequestDelete() {
    }

    public ClientRequestDelete(String destinationServer, String table, String key) {
        super(destinationServer, table, key);
    }

    public ClientRequestDelete(String destinationNode,
                               String table,
                               String key,
                               WriteConcern writeConcern) {
        super(destinationNode, table, key, writeConcern);
    }
}
