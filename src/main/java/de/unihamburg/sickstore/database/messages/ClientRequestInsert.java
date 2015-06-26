package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;

public class ClientRequestInsert extends ClientRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String destinationServer, String table, String key, Version version) {
        super(destinationServer, table, key);
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
