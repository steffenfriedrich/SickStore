package de.unihamburg.sickstore.backend.anomaly.replicationdelay;

import de.unihamburg.sickstore.database.messages.ClientRequest;
import java.util.Set;

public interface ReplicationDelayGenerator {

    /**
     * Calculates the time a specific write request needs to complete
     * (the time delay until it would be finished).
     *
     * @param servers    set with all server ids
     * @param request
     * @return the returned value describes how long the request should be delayed until
     *          the request is finished
     */
    long calculateDelay(Set<Integer> servers, ClientRequest request);
}
