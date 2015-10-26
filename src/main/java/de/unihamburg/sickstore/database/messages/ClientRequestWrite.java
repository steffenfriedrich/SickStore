package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

public abstract class ClientRequestWrite extends ClientRequest {

    private WriteConcern writeConcern = new WriteConcern();

    public ClientRequestWrite() {
    }

    public ClientRequestWrite(String table, String key) {
        super(table, key);
    }

    public ClientRequestWrite(String table, String key, String destinationNode) {
        super(table, key, destinationNode);
    }

    public ClientRequestWrite(String table, String key, WriteConcern writeConcern) {
        super(table, key);

        this.writeConcern = writeConcern;
    }

    public ClientRequestWrite(String table, String key, WriteConcern writeConcern, String destinationNode) {
        super(table, key, destinationNode);

        this.writeConcern = writeConcern;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    @Override
    public String toString() {
        return "WRITE";
    }
}
