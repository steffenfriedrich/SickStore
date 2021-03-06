package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.backend.Version;

public class ServerResponseCleanup extends ServerResponse {

    @SuppressWarnings("unused")
    private ServerResponseCleanup() {
        super();
    }

    public ServerResponseCleanup(int clientRequestID) {
        super(clientRequestID);
    }

    @Override
    public String toString() {
        return "CLEANUP";
    }
}
