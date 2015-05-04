package de.unihamburg.sickstore.database.messages;

import java.util.concurrent.atomic.AtomicLong;

public class ServerResponse {

    protected static final AtomicLong counter = new AtomicLong();

    /** The request ID of the client request that triggered this response */
    protected Long clientRequestID;

    /** Atomic ID of this response (which increases with each response) */
    protected Long responseId;

    /** Indicates how long the requesting client needs to delay after this response (to simulate write latencies) */
    protected Long waitTimeout = 0l;

    public ServerResponse() {
        super();

        this.responseId = counter.incrementAndGet();
    }

    public ServerResponse(long clientRequestID) {
        this();

        this.clientRequestID = clientRequestID;
    }

    public Long getClientRequestID() {
        return clientRequestID;
    }

    public Long getResponseId() {
        return responseId;
    }

    public Long getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(Long waitTimeout) {
        this.waitTimeout = waitTimeout;
    }
}