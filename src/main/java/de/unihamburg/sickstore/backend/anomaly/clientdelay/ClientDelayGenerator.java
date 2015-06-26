package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import java.util.Set;

public interface ClientDelayGenerator {

    /**
     * Calculates the time a specific write request needs to complete
     * (the time delay until it would be finished).
     *
     * @param request
     * @param nodes    set with all nodes the write is propagated to
     * @return the returned value describes how long the request should be delayed until
     *          the request is finished
     */
    long calculateDelay(ClientRequest request, Set<Node> nodes);
}
