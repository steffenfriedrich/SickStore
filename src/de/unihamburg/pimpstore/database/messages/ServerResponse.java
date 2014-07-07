package de.unihamburg.pimpstore.database.messages;

import java.util.concurrent.atomic.AtomicLong;

public class ServerResponse {

    protected static final AtomicLong counter = new AtomicLong();

    /** The request ID of the client request that triggered this response */
    protected Long clientRequestID;

    /** The request ID of the client request that triggered this response */
    protected Long id;

    public ServerResponse() {
        super();
    }

    public ServerResponse(long clientRequestID) {
        this.clientRequestID = clientRequestID;
        this.id = counter.incrementAndGet();
    }

    public Long getClientRequestID() {
        return clientRequestID;
    }

    public Long getId() {
        return id;
    }
}