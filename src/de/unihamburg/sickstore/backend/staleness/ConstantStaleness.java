/**
 * 
 */
package de.unihamburg.sickstore.backend.staleness;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.database.messages.ClientRequest;

/**
 * @author Wolfram Wingerath
 * 
 */
public class ConstantStaleness implements StalenessGenerator {

    /**
     * the staleness in ms that a server experiences when reading data written
     * by another server
     */
    private long foreignReads = 0;

    /**
     * the staleness in ms that a server experiences when reading data written
     * by itself
     */
    private long ownReads = 0;

    public ConstantStaleness(long foreignReads, long ownReads) {
        super();
        this.foreignReads = foreignReads;
        this.ownReads = ownReads;
    }

    /**
     * @see de.unihamburg.sickstore.backend.staleness.StalenessGenerator#get(Set, int, ClientRequest) 
     */
    public Map<Integer, Long> get(Set<Integer> servers, int readingServer, ClientRequest request) {
        HashMap<Integer, Long> delay = new HashMap<Integer, Long>();

        for (Integer server : servers) {
            if (server == readingServer) {
                delay.put(server, ownReads);
            } else {
                delay.put(server, foreignReads);
            }
        }

        return delay;
    }
}
