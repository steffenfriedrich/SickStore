package database.messages;

import java.util.Set;

public class ClientRequestRead extends ClientRequest { 
    private  Set<String> fields;

    private ClientRequestRead() {
    }

    public ClientRequestRead(String table, String key, Set<String> fields) {
        super(table, key); 
       this.fields = fields;
    } 
}
