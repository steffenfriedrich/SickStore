package database.messages;

import java.util.List;

import backend.Entry;

public class ServerResponseScan extends ServerResponse {
    private List<Entry> entries;

    private ServerResponseScan() {
    }

    public ServerResponseScan(Long id, List<Entry> entries) {
        super();
        this.id = id;
        this.entries =  entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    } 
}
