package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.WriteConcern;

public class ClientRequestUpdate extends ClientRequestWrite {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestUpdate() {
    }

    public ClientRequestUpdate(String table, String key, Version version, String destinationNode) {
        super(table, key, destinationNode);
        this.version = version;
    }

    public ClientRequestUpdate(String table, String key, Version version, WriteConcern writeConcern) {
        super(table, key, writeConcern);
        this.version = version;
    }

    public ClientRequestUpdate(String table, String key, Version version, WriteConcern writeConcern, String destinationNode) {
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
        return "UPDATE";
    }
}
