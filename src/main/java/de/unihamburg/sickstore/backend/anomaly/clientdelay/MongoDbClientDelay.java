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
    private Set<NetworkDelay> customDelays = new HashSet<>();

    private TimeHandler timeHandler = new SystemTimeHandler();

    // Tagset Name => tag + its replica acknowledgment
    private HashMap<String, HashMap<String, Integer>> tagSets = new HashMap<>();

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
                              Set<NetworkDelay> customDelays,
                              HashMap<String, HashMap<String, Integer>> tagSets,
                              TimeHandler timeHandler) {
        this.defaultDelay = defaultDelay;
        this.journalCommitInterval = journalCommitInterval;
        this.customDelays = customDelays;
        this.tagSets = tagSets;
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

    /**
     * Calculates the actual write delay, which is the maximum of the replication delay
     * or the journaling delay.
     *
     * @param request
     * @param nodes
     * @return
     */
    private long calculateWriteDelay(ClientWriteRequest request, Set<Node> nodes) {
        long replicationDelay = calculateReplicationDelay(request, nodes);
        long journalingDelay = calculateJournalingDelay(request);

        return Math.max(replicationDelay, journalingDelay);
    }

    /**
     * Calculates the write delay that is caused by a journal commmit.
     *
     * @param request
     * @return
     */
    private long calculateJournalingDelay(ClientWriteRequest request) {
        if (!request.getWriteConcern().isJournaling()) {
            return 0;
        }

        long timeSinceStartup = timeHandler.getCurrentTime() - startedAt;
        long oneThird = journalCommitInterval / 3;

        return oneThird - (timeSinceStartup % oneThird);
    }

    /**
     * Calculates the replica that is caused by replicating the operation to the required
     * number of nodes (or all nodes which match the tag-concern)
     *
     * @param request
     * @param nodes
     * @return
     */
    private long calculateReplicationDelay(ClientWriteRequest request, Set<Node> nodes) {
        WriteConcern writeConcern = request.getWriteConcern();
        if (writeConcern.getReplicaAcknowledgementTagSet() != null) {
            return calculateTaggedReplicationDelay(request, nodes);
        }

        if ((nodes.size() == 1 || writeConcern.getReplicaAcknowledgement() <= 1)) {
            // if there is only one node or if it should not be waited for
            // further replica acknowledgments the delay is zero
            return 0;
        }

        // select custom delays
        TreeSet<Long> delays = findRelevantReplicationDelays(request.getReceivedBy());

        // the primary is subtracted (-1), as it has no delay
        return calculateActualReplicationDelay(writeConcern.getReplicaAcknowledgement() - 1, delays);
    }

    /**
     * Calculate the delay for the request, that is produced by the write concern for a tag.
     *
     * @param request
     * @param nodes
     * @return
     */
    private long calculateTaggedReplicationDelay(ClientWriteRequest request, Set<Node> nodes) {
        WriteConcern writeConcern = request.getWriteConcern();

        if (!tagSets.containsKey(writeConcern.getReplicaAcknowledgementTagSet())) {
            throw new IndexOutOfBoundsException("There is no tag-concern with name " +
                writeConcern.getReplicaAcknowledgementTagSet());
        }

        HashMap<String, Integer> tagSet = tagSets.get(writeConcern.getReplicaAcknowledgementTagSet());

        long delay = 0;
        for (HashMap.Entry<String, Integer> tagsetEntry : tagSet.entrySet()) {
            String tag = tagsetEntry.getKey();

            int acknowledgment = tagsetEntry.getValue();
            if (request.getReceivedBy().getTags().contains(tag)) {
                // if the receiving node has the current tag, reduce the number of acknowledgments
                acknowledgment--;
            }

            // find custom delays of nodes with the current tag
            TreeSet<Long> relevantDelays = findRelevantReplicationDelaysWithTag(
                tag,
                request.getReceivedBy(),
                nodes
            );

            // calculate delay for this tag
            long tagDelay = calculateActualReplicationDelay(acknowledgment, relevantDelays);
            if (tagDelay > delay) {
                delay = tagDelay;
            }
        }

        return delay;
    }

    /**
     * Returns a sorted list of all delays, that occur from the receiving node
     * to its replicas with a specific tag.
     *
     * @param receivedBy
     * @return a sorted list with all delays
     */
    private TreeSet<Long> findRelevantReplicationDelaysWithTag(String tag,
                                                               Node receivedBy,
                                                               Set<Node> nodes) {
        // Find delays of nodes with that tag
        TreeSet<Long> delays = new TreeSet<>();
        for (Node node : nodes) {
            if (node.getTags().contains(tag)) {
                if (node == receivedBy) {
                    // the receiving node has no extra delay
                    continue;
                }

                for (NetworkDelay customDelay : customDelays) {
                    if (customDelay.getFrom() == receivedBy && customDelay.getTo() == node) {
                        delays.add(customDelay.getDelay());
                    }
                }
            }
        }

        return delays;
    }

    /**
     * Returns a sorted list of all delays, that occur from the receiving node to its replicas.
     *
     * @param receivedBy
     * @return a sorted list with all delays
     */
    private TreeSet<Long> findRelevantReplicationDelays(Node receivedBy) {
        TreeSet<Long> delays = new TreeSet<>();

        for (NetworkDelay customDelay : customDelays) {
            if (customDelay.getFrom() == receivedBy) {
                // there is custom delay between the node which received the request
                // and another node

                delays.add(customDelay.getDelay());
            }
        }

        return delays;
    }

    /**
     * Calculates the actual delay that will be produced by the write concern.
     * @param acknowledgments number of acknowledgments that are necessary
     * @param delays
     * @return
     */
    private long calculateActualReplicationDelay(int acknowledgments, TreeSet<Long> delays) {

        Iterator<Long> it = delays.iterator();
        long delay = 0;
        int acknowledgmentsLeft = acknowledgments;
        while (it.hasNext() && acknowledgmentsLeft > 0) {
            Long tmpDelay = it.next();
            if (tmpDelay > delay) {
                delay = tmpDelay;
            }

            acknowledgmentsLeft--;
        }

        if (acknowledgmentsLeft > 0 && delay < defaultDelay) {
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

    public void setCustomDelays(Set<NetworkDelay> customDelays) {
        this.customDelays = customDelays;
    }

    public void setTagSets(HashMap<String, HashMap<String, Integer>> tagSets) {
        this.tagSets = tagSets;
    }

    public void setTimeHandler(TimeHandler timeHandler) {
        this.timeHandler = timeHandler;
    }
}