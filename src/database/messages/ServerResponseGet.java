package database.messages;

import backend.Entry;

public class ServerResponseGet extends ServerResponse {
    private Entry entry;

    private ServerResponseGet() {
    }

    public ServerResponseGet(Long id, Entry entry) {
        super();
        this.id = id;
        this.entry = entry;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }
}
