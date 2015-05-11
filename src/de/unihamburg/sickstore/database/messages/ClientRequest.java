package de.unihamburg.sickstore.database.messages;

import java.util.concurrent.atomic.AtomicLong;

public abstract class ClientRequest {

    protected static final AtomicLong counter = new AtomicLong();

    protected Long id;

    protected String key;

    /**
     * a timestamp indicating when the server received the request. Has to be
     * set by the receiving server
     */
    private long receivedAt = -1;

    /**
     * Indicates what server received the request. Has to be set by the
     * receiving server
     */
    private int receivedBy = -1;

    protected String table;

    public ClientRequest() {
        super();
    }

    public ClientRequest(String table, String key) {
        this();
        this.table = table;
        this.key = key;
        this.id = counter.incrementAndGet();
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public int getReceivedBy() {
        return receivedBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setReceivedBy(int receivedBy) {
        this.receivedBy = receivedBy;
    }
}