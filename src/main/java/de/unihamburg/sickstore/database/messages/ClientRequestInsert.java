package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestInsert extends ClientWriteRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String destinationServer, String table, String key, Version version) {
        super(destinationServer, table, key);
        this.version = version;
    }

    public ClientRequestInsert(String destinationNode,
                               String table,
                               String key,
                               Version version,
                               WriteConcern writeConcern) {
        super(destinationNode, table, key, writeConcern);
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
