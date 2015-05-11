package de.unihamburg.sickstore.backend.anomaly.replicationdelay;

import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.Set;

public class MongoDbReplicationDelay implements ReplicationDelayGenerator {

    /** time needed to contact a replica until the response arrives */
    private long defaultDelay;

    /**
     *
     * @param defaultDelay time needed (in ms) to send data to a replica until the
     *                     response arrives
     */
    public MongoDbReplicationDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    /**
     * @see ReplicationDelayGenerator#calculateDelay(Set, ClientRequest)
     */
    @Override
    public long calculateDelay(Set<Integer> servers, ClientRequest request) {
        if (servers.size() > 0) {
            // requests to replicas are executed in parallel,so it doesn't matter how many replica
            // there are, it always takes the same time
            return defaultDelay;
        }

        return 0;
    }
}