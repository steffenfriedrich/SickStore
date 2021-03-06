/**
 * 
 */
package de.unihamburg.sickstore.backend.anomaly.staleness;

import java.util.Map;
import java.util.Set;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;

/**
 * @author Wolfram Wingerath
 * 
 */
public class ConstantStaleness implements StalenessGenerator {

    /**
     * the staleness in ms that a node experiences when reading data written
     * by another node
     */
    private long foreignReads = 0;

    /**
     * the staleness in ms that a node experiences when reading data written
     * by itself
     */
    private long ownReads = 0;

    @SuppressWarnings("unused")
    public static ConstantStaleness newInstanceFromConfig(Map<String, Object> config) {
        return new ConstantStaleness(
            Long.valueOf((Integer) config.get("foreignReads")),
            Long.valueOf((Integer) config.get("ownReads"))
        );
    }

    public ConstantStaleness(long foreignReads, long ownReads) {
        super();
        this.foreignReads = foreignReads;
        this.ownReads = ownReads;
    }

    /**
     * @see StalenessGenerator#generateStalenessMap(Set, ClientRequest)
     */
    public StalenessMap generateStalenessMap(Set<Node> nodes, ClientRequest request) {
        StalenessMap delay = new StalenessMap();

        for (Node node : nodes) {
            if (node == request.getReceivedBy()) {
                delay.put(node, ownReads);
            } else {
                delay.put(node, foreignReads);
            }
        }
        return delay;
    }
}
