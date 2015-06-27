package de.unihamburg.sickstore.database;

public class WriteConcern {

    private int replicaAcknowledgement = 1;
    private boolean journaling = false;
    private int timeout = 0;

    public WriteConcern() {
    }

    public WriteConcern(int replicaAcknowledgement) {
        this.replicaAcknowledgement = replicaAcknowledgement;
    }

    public WriteConcern(int replicaAcknowledgement, boolean journaling, int timeout) {
        this.replicaAcknowledgement = replicaAcknowledgement;
        this.journaling = journaling;
        this.timeout = timeout;
    }

    public int getReplicaAcknowledgement() {
        return replicaAcknowledgement;
    }

    public void setReplicaAcknowledgement(int replicaAcknowledgement) {
        this.replicaAcknowledgement = replicaAcknowledgement;
    }

    public boolean isJournaling() {
        return journaling;
    }

    public void setJournaling(boolean journaling) {
        this.journaling = journaling;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
