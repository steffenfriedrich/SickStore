package database.messages;

import java.util.List;

import backend.Version;

public class ServerResponseScan extends ServerResponse {
    private List<Version> entries;

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
