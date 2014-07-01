package database.messages;

import java.util.concurrent.atomic.AtomicLong;

public class ClientRequest {

    protected static final AtomicLong counter = new AtomicLong();
    protected String table;
    protected String key;
    protected Long id;

    public ClientRequest() { 
        super();
        this.id = counter.incrementAndGet();
    } 

    public ClientRequest(String table, String key) {
        this();
        this.table = table;
        this.key = key;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}