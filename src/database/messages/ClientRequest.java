package database.messages;

import java.util.concurrent.atomic.AtomicLong;

public class ClientRequest {

    /**
     * a timestamp indicating when the server received the request. Has to be
     * set by the receiving server
     */
    private long receivedAt = -1;

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    /**
     * Indicates what server received the request. Has to be set by the
     * receiving server
     */
    private int receivedBy = -1;

    public int getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(int receivedBy) {
        this.receivedBy = receivedBy;
    }

    protected static final AtomicLong counter = new AtomicLong();
    protected String table;
    protected String key;
    protected Long id;

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