package database.messages;

import backend.Version;

public class ClientRequestInsert extends ClientRequest {
    private Version version;

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @SuppressWarnings("unused")
    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String table, String key, Version version) {
        super(table, key);
        this.version = version;
    }
}
