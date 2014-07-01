package database.messages;

import java.util.Set;

public class ClientRequestScan extends ClientRequest { 
    private int recordcount;
    private  Set<String> fields;

    private ClientRequestScan() {
    }

    public ClientRequestScan(String table, String key, int recordcount, Set<String> fields) {
        super(table, key); 
        this.recordcount = recordcount;
       this.fields = fields;
    } 
}
