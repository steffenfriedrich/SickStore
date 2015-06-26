package de.unihamburg.sickstore.backend.anomaly.staleness;

import java.util.Map;
import java.util.Set;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    /**
     * Calculates the staleness windows for a changed data item (insert, update, delete).
     *
     * The returned map associates a node with a delay, after which the item becomes visible.
     *
     * @param nodes a set with all nodes
     * @param request the change request
     * @return a Map of Server to Delay
     */
    StalenessMap get(Set<Node> nodes, ClientRequest request);
}