package de.unihamburg.sickstore.backend.anomaly.staleness;

import de.unihamburg.sickstore.database.Node;

import java.util.HashMap;

public class StalenessMap extends HashMap<Node, Long> {

    public Long getByServerId(int server) {
        Long staleness = null;
        for (Node node : this.keySet()) {
            if (node.getId() == server) {
                staleness = this.get(node);
            }
        }

        return staleness;
    }

}
