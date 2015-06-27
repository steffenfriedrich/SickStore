package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

public abstract class ClientWriteRequest extends ClientRequest {

    private WriteConcern writeConcern = new WriteConcern();

    public ClientWriteRequest() {
    }

    public ClientWriteRequest(String destinationNode, String table, String key) {
        super(destinationNode, table, key);
    }

    public ClientWriteRequest(String destinationNode,
                              String table,
                              String key,
                              WriteConcern writeConcern) {
        super(destinationNode, table, key);

        this.writeConcern = writeConcern;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }
}
