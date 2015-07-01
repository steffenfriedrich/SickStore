package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

public abstract class ClientWriteRequest extends ClientRequest {

    private WriteConcern writeConcern = new WriteConcern();

    public ClientWriteRequest() {
    }

    public ClientWriteRequest(String table, String key) {
        super(table, key);
    }

    public ClientWriteRequest(String table, String key, String destinationNode) {
        super(table, key, destinationNode);
    }

    public ClientWriteRequest(String table, String key, WriteConcern writeConcern) {
        super(table, key);

        this.writeConcern = writeConcern;
    }

    public ClientWriteRequest(String table, String key, WriteConcern writeConcern, String destinationNode) {
        super(table, key, destinationNode);

        this.writeConcern = writeConcern;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }
}
