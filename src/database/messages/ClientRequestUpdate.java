package database.messages;

import backend.Entry;

public class ClientRequestUpdate extends ClientRequest { 
    private Entry entry;

    private ClientRequestUpdate() {
    }

    public ClientRequestUpdate(String table, String key, Entry entry) {
        super(table, key); 
        this.entry = entry;
    } 
}
