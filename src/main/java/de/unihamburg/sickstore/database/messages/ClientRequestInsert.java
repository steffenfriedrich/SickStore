package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestInsert extends ClientWriteRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String table, String key, Version version) {
        super(table, key);
        this.version = version;
    }

    public ClientRequestInsert(String table, String key, Version version, String destinationServer) {
        super(table, key, destinationServer);
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
}
