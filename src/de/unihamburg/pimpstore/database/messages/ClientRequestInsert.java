package de.unihamburg.pimpstore.database.messages;

import de.unihamburg.pimpstore.backend.Version;

public class ClientRequestInsert extends ClientRequest {
    private Version version;

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String table, String key, Version version) {
        super(table, key);
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
