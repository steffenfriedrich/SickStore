package de.unihamburg.sickstore.backend.anomaly.staleness;

import java.util.Map;
import java.util.Set;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    /**
     * Calculates the staleness windows for a changed data item (insert, update, delete).
     *
     * The returned map associates a server's id with a delay, after which the item becomes visible.
     *
     * @param servers a set with all server ids
     * @param request the change request
     * @return a Map of Server to Delay
     */
    StalenessMap get(Set<Node> servers, ClientRequest request);
}