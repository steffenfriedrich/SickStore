/**
 * 
 */
package backend.staleness;

import java.util.HashMap;
import java.util.Map;

import backend.QueryHandler;
import database.messages.ClientRequest;

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
     * @see backend.staleness.StalenessGenerator#get(int,
     *      database.messages.ClientRequest)
     */
    @Override
    public Map<Integer, Long> get(int server, ClientRequest request) {
        QueryHandler handler = QueryHandler.getInstance();
        HashMap<Integer, Long> delay = new HashMap<Integer, Long>();

        for (Integer s : handler.getServers()) {
            if (s == server) {
                delay.put(s, ownReads);
            } else {
                delay.put(s, foreignReads);
            }
        }

        return delay;
    }
}
