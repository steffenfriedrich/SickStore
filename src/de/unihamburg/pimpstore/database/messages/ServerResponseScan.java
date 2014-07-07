package de.unihamburg.pimpstore.database.messages;

import java.util.List;

import de.unihamburg.pimpstore.backend.Version;

public class ServerResponseScan extends ServerResponse {
    private List<Version> entries;

    @SuppressWarnings("unused")
    private ServerResponseScan() {
        super();
    }

    public ServerResponseScan(long clientRequestID, List<Version> entries) {
        super(clientRequestID);
        this.entries = entries;
    }

    public List<Version> getEntries() {
        return entries;
    }

    public void setEntries(List<Version> entries) {
        this.entries = entries;
    }
}
