package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestUpdate extends ClientWriteRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestUpdate() {
    }

    public ClientRequestUpdate(String destinationServer, String table, String key, Version version) {
        super(destinationServer, table, key);
        this.version = version;
    }

    public ClientRequestUpdate(String destinationNode,
                               String table,
                               String key,
                               WriteConcern writeConcern) {
        super(destinationNode, table, key, writeConcern);
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
