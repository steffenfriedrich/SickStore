package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestInsert extends ClientRequestWrite {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String table, String key, Version version) {
        super(table, key);
        this.version = version;
    }

    public ClientRequestInsert(String table, String key, Version version, String destinationNode) {
        super(table, key, destinationNode);
        this.version = version;
    }

    public ClientRequestInsert(String table, String key, Version version, WriteConcern writeConcern) {
        super(table, key, writeConcern);
        this.version = version;
    }

    public ClientRequestInsert(String table, String key, Version version, WriteConcern writeConcern, String destinationNode) {
        super(table, key, writeConcern, destinationNode);
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "INSERT";
    }
}
