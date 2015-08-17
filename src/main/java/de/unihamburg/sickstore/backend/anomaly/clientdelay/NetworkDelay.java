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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkDelay that = (NetworkDelay) o;

        if (latency != that.latency) return false;
        if (!from.equals(that.from)) return false;

        return to.equals(that.to);

    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();

        return result;
    }
}
