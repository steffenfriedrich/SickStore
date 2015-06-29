package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestUpdate extends ClientWriteRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestUpdate() {
    }

    public ClientRequestUpdate(String table, String key, Version version, String destinationServer) {
        super(table, key, destinationServer);
        this.version = version;
    }

    public ClientRequestUpdate(String table, String key, WriteConcern writeConcern) {
        super(table, key, writeConcern);
    }

    public ClientRequestUpdate(String table, String key, WriteConcern writeConcern, String destinationNode) {
        super(table, key, writeConcern, destinationNode);
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
