package database.messages;

import backend.Entry;

public class ClientRequestInsert extends ClientRequest { 
    private Entry entry;

    private ClientRequestInsert() {
    }

    public ClientRequestInsert(String table, String key, Entry entry) {
        super(table, key); 
        this.entry = entry;
    } 
}
