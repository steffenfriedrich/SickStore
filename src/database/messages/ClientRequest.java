package database.messages;

import java.util.concurrent.atomic.AtomicLong;

public class ClientRequest {

    protected static final AtomicLong counter = new AtomicLong();
    protected String key;
    protected Long id;

    public ClientRequest() {
        super();
        this.id = counter.incrementAndGet();
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