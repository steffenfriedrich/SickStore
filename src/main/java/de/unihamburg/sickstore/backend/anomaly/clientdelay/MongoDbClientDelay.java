package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientWriteRequest;

import java.util.*;

/**
 * This class calculates a delay for write request which is caused by a MongoDB-like replication.
 */
public class MongoDbClientDelay implements ClientDelayGenerator {

    /** time needed to contact a replica until the response arrives */
    private long defaultDelay = 0;

    /** time interval in which the journal is committed */
    private long journalCommitInterval = 100;

    /** custom delays between two nodes */
    private Map<Node[], Long> customDelays = new HashMap<>();

    private TimeHandler timeHandler = new SystemTimeHandler();

    private long startedAt;

    /**
     */
    public MongoDbClientDelay() {
    }

    /**
     */
    public MongoDbClientDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
        this.startedAt = timeHandler.getCurrentTime();
    }

    /**
     *
     * @param defaultDelay time needed (in ms) to send data to a replica until the
     *                     response arrives
     */
    public MongoDbClientDelay(long defaultDelay,
                              long journalCommitInterval,
                              Map<Node[], Long> customDelays,
                              TimeHandler timeHandler) {
        this.defaultDelay = defaultDelay;
        this.journalCommitInterval = journalCommitInterval;
        this.customDelays = customDelays;
        this.timeHandler = timeHandler;
        this.startedAt = timeHandler.getCurrentTime();
    }

    /**
     * @see ClientDelayGenerator#calculateDelay(ClientRequest, Set)
     */
    @Override
    public long calculateDelay(ClientRequest request, Set<Node> nodes) {
        if (request instanceof ClientWriteRequest) {
            return calculateWriteDelay((ClientWriteRequest) request, nodes);
        }

        return 0;
    }

    private long calculateWriteDelay(ClientWriteRequest request, Set<Node> nodes) {
        long replicationDelay = calculateReplicationDelay(request, nodes);
        long journalingDelay = calculateJournalingDelay(request);

        return Math.max(replicationDelay, journalingDelay);
    }

    private long calculateJournalingDelay(ClientWriteRequest request) {
        if (!request.getWriteConcern().isJournaling()) {
            return 0;
        }

        long timeSinceStartup = timeHandler.getCurrentTime() - startedAt;
        long oneThird = journalCommitInterval / 3;

        long nextCommit = oneThird - (timeSinceStartup % oneThird);

        return nextCommit;
    }

    private long calculateReplicationDelay(ClientWriteRequest request, Set<Node> nodes) {
        WriteConcern writeConcern = request.getWriteConcern();
        if ((nodes.size() == 1 || writeConcern.getReplicaAcknowledgement() <= 1)) {
            // if there is only one node or it should not be waited for replica acknowledgements
            // there delay is zero
            return 0;
        }

        if (customDelays.size() == 0) {
            return defaultDelay;
        }

        // check whether there are custom delays
        Set<Long> delays = findRelevantReplicationDelays(request.getReceivedBy());

        return calculateActualReplicationDelay(writeConcern, delays);
    }

    /**
     * Returns a sorted list of all delays, that occur from the receiving nodes to their replica.
     *
     * @param receivedBy
     * @return a sorted list with all delays
     */
    private TreeSet<Long> findRelevantReplicationDelays(Node receivedBy) {
        TreeSet<Long> delays = new TreeSet<>();

        for (Map.Entry<Node[], Long> customDelay : customDelays.entrySet()) {
            Node[] delayBetween = customDelay.getKey();
            if (delayBetween[0] == receivedBy) {
                // there is custom delay between the node which received the request
                // and another node

                delays.add(customDelay.getValue());
            }
        }

        return delays;
    }

    /**
     * Calculates the actual delay that will be produced by the write concern.
     * @param writeConcern
     * @param delays
     * @return
     */
    private long calculateActualReplicationDelay(WriteConcern writeConcern, Set<Long> delays) {

        Iterator<Long> it = delays.iterator();
        long delay = 0;
        int acknowledgementsLeft = writeConcern.getReplicaAcknowledgement() - 1; // the primary is substracted, as it has no delay
        while (it.hasNext() && acknowledgementsLeft > 0) {
            Long tmpDelay = it.next();
            if (tmpDelay > delay) {
                delay = tmpDelay;
            }

            acknowledgementsLeft--;
        }

        if (acknowledgementsLeft > 0 && delay < defaultDelay) {
            return defaultDelay;
        }

        return delay;
    }

    public void setDefaultDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    public void setJournalCommitInterval(long journalCommitInterval) {
        this.journalCommitInterval = journalCommitInterval;
    }

    public void setCustomDelays(Map<Node[], Long> customDelays) {
        this.customDelays = customDelays;
    }

    public void setTimeHandler(TimeHandler timeHandler) {
        this.timeHandler = timeHandler;
    }
}