package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.Node;

public class NetworkDelay {

    private Node from;
    private Node to;
    private long latency;

    public NetworkDelay(Node from, Node to, long latency) {
        this.from = from;
        this.to = to;
        this.latency = latency;
    }

    public Node getFrom() {
        return from;
    }

    public void setFrom(Node from) {
        this.from = from;
    }

    public Node getTo() {
        return to;
    }

    public void setTo(Node to) {
        this.to = to;
    }

    public long getDelay() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }
}
