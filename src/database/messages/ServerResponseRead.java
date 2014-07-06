package database.messages;

import backend.Version;

public class ServerResponseRead extends ServerResponse {
    private Version version;

    @SuppressWarnings("unused")
    private ServerResponseRead() {
        super();
    }

    public ServerResponseRead(long clientRequestID, Version version) {
        super(clientRequestID);
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
