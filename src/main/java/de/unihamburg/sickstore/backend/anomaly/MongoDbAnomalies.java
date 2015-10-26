package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.NetworkDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestWrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class calculates a delay for write request which is caused by a MongoDB-like replication.
 */
public class MongoDbAnomalies implements ClientDelayGenerator, StalenessGenerator {

    private static final Logger log = LoggerFactory.getLogger("sickstore");

    /** time needed to contact a replica until the response arrives */
    private long defaultDelay = 0;

    /** time interval in which the journal is committed */
    private long journalCommitInterval = 100;

    /** delays between client and nodes */
    private Set<NetworkDelay> clientNodeLatencies = new HashSet<>();

    /** custom delays between two nodes */
    private Set<NetworkDelay> customDelays = new HashSet<>();

    private TimeHandler timeHandler = new SystemTimeHandler();

    // Tagset Name => tag + its replica acknowledgment
    private Map<String, Map<String, Integer>> tagSets = new HashMap<>();

    private long startedAt;

    @SuppressWarnings("unused")
    public static MongoDbAnomalies newInstanceFromConfig(Map<String, Object> config) {

        Set<Node> nodes = (Set<Node>) config.get("nodes");
        Set<NetworkDelay> clientNodeLatencies = new HashSet<>();

        List<List<Object>> clientNodeLatenciesConfig = (List<List<Object>>) config.
                getOrDefault("clientNodeLatencies", new ArrayList<>());
        
        for (List<Object> clientNodeLatency : clientNodeLatenciesConfig) {
            String serverName = (String) clientNodeLatency.get(0);

            Node server = null;
            // search nodes
            for (Node node : nodes) {
                if (node.getName().equals(serverName)) {
                    server = node;
                }
            }
            if (server == null) {
                throw new RuntimeException("No node found for delay between client and server node (" + server + ")");
            }
            NetworkDelay delay = new NetworkDelay(new Node("Client"), server, (int) clientNodeLatency.get(1));
            clientNodeLatencies.add(delay);
        }


        Set<NetworkDelay> customDelays = new HashSet<>();

        List<List<Object>> customDelayConfig = (List<List<Object>>) config.
            getOrDefault("customDelays", new ArrayList<>());

        for (List<Object> customDelay : customDelayConfig) {
            String fromName = (String) customDelay.get(0);
            String toName = (String) customDelay.get(1);

            Node to = null;
            Node from = null;

            // search nodes
            for (Node node : nodes) {
                if (node.getName().equals(fromName)) {
                    from = node;
                }
                if (node.getName().equals(toName)) {
                    to = node;
                }
            }

            if (from == null) {
                throw new RuntimeException("No node found for from: " + fromName);
            }
            if (to == null) {
                throw new RuntimeException("No node found for to: " + toName);
            }

            customDelays.add(new NetworkDelay(from, to, (int) customDelay.get(2)));
        }

        return new MongoDbAnomalies(
            (int) config.getOrDefault("defaultDelay", 0),
            (int) config.getOrDefault("journalCommitInterval", 0),
                clientNodeLatencies,
            customDelays,
            (Map<String, Map<String, Integer>>) config.getOrDefault("tagSets", new HashMap<>())
        );
    }

    /**
     */
    public MongoDbAnomalies() {
        this.startedAt = timeHandler.getCurrentTime();
    }

    /**
     */
    public MongoDbAnomalies(long defaultDelay) {
        this.defaultDelay = defaultDelay;
        this.startedAt = timeHandler.getCurrentTime();
    }

    /**
     *
     * @param defaultDelay             default delay (in ms) between two nodes.
     * @param journalCommitInterval
     * @param customDelays             custom delays between two nodes
     * @param tagSets
     */
    public MongoDbAnomalies(long defaultDelay,
                            long journalCommitInterval,
                            Set<NetworkDelay> clientNodeLatencies,
                            Set<NetworkDelay> customDelays,
                            Map<String, Map<String, Integer>> tagSets) {
        this(defaultDelay, journalCommitInterval, clientNodeLatencies, customDelays, tagSets, new SystemTimeHandler());
    }

    /**
     *
     */
    public MongoDbAnomalies(long defaultDelay,
                            long journalCommitInterval,
                            Set<NetworkDelay> clientNodeLatencies,
                            Set<NetworkDelay> customDelays,
                            Map<String, Map<String, Integer>> tagSets,
                            TimeHandler timeHandler) {
        this.defaultDelay = defaultDelay;
        this.journalCommitInterval = journalCommitInterval;
        this.clientNodeLatencies = clientNodeLatencies;
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
        long clientServerDelay = calculateClientServerDelay(request, nodes);
        long writeDelay = 0;
        if (request instanceof ClientRequestWrite) {
            writeDelay = calculateWriteDelay((ClientRequestWrite) request, nodes);
        }
        return clientServerDelay + writeDelay;
    }

    /**
     * Calculates the delay between client and requested node
     * @param request
     * @param nodes
     * @return
     */
    private long calculateClientServerDelay(ClientRequest request, Set<Node> nodes) {
        Node receivedBy = request.getReceivedBy();

        for (NetworkDelay delay : clientNodeLatencies) {
            if (delay.getTo() == receivedBy) {
                return delay.getDelay();
            }
        }
        return 0;
    }


    /**
     * Calculates the staleness map for the passed request.
     *
     * @param nodes a set with all nodes
     * @param request the change request
     * @return
     */
    @Override
    public StalenessMap generateStalenessMap(Set<Node> nodes, ClientRequest request) {
        long start = System.currentTimeMillis();
        StalenessMap stalenessMap = new StalenessMap();

        for (Node node : nodes) {
            long staleness = -1;
            // find custom delay to replica (which is also the staleness value)

            if (node == request.getReceivedBy()) {
                // request arrived already on the primary, so there is no staleness
                staleness = 0;
            } else {
                // find the delay that occurs until the request arrives at the replica
                for (NetworkDelay delay : customDelays) {
                    if (delay.getFrom() == request.getReceivedBy() && node == delay.getTo()) {
                        staleness = delay.getDelay();

                        break;
                    }
                }
            }

            // no custom delay found, use the default one
            if (staleness == -1) {
                staleness = defaultDelay;
            }

            stalenessMap.put(node, staleness);
        }
        long end = System.currentTimeMillis();
        log.debug("MongoDbAnomalies,generateStalenessMap,{},{}",request.toString(), (end - start));
        return stalenessMap;
    }

    /**
     * Calculates the actual write delay, which is the maximum of the replication delay
     * or the journaling delay.
     *
     * @param request
     * @param nodes
     * @return
     */
    private long calculateWriteDelay(ClientRequestWrite request, Set<Node> nodes) {
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
    private long calculateJournalingDelay(ClientRequestWrite request) {
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
    private long calculateReplicationDelay(ClientRequestWrite request, Set<Node> nodes) {
        long start = System.currentTimeMillis();
        WriteConcern writeConcern = request.getWriteConcern();
        if (writeConcern.getReplicaAcknowledgementTagSet() != null) {
            return calculateTaggedReplicationDelay(request, nodes);
        }

        if ((nodes.size() == 1 || writeConcern.getReplicaAcknowledgement() <= 1)) {
            // if there is only one node or if no replica acknowledgment is required
            // the delay is zero
            return 0;
        }

        // look for custom delays
        TreeSet<Long> delays = findCustomDelays(request.getReceivedBy(), nodes);
        long end = System.currentTimeMillis();
        log.debug("MongoDbAnomalies,calculateReplicationDelay,{},{}",request.toString(), (end - start));
        // the primary is subtracted (-1), as it has no delay
        return calculateObservableReplicationDelay(
            writeConcern.getReplicaAcknowledgement() - 1,
            delays
        );
    }

    /**
     * Calculate the delay for the request, that is produced by the write concern for a tag.
     *
     * @param request
     * @param nodes
     * @return
     */
    private long calculateTaggedReplicationDelay(ClientRequestWrite request, Set<Node> nodes) {
        long start = System.currentTimeMillis();
        WriteConcern writeConcern = request.getWriteConcern();

        if (!tagSets.containsKey(writeConcern.getReplicaAcknowledgementTagSet())) {
            throw new IndexOutOfBoundsException("There is no tag-concern with name " +
                writeConcern.getReplicaAcknowledgementTagSet());
        }

        Map<String, Integer> tagSet = tagSets.get(writeConcern.getReplicaAcknowledgementTagSet());

        long delay = 0;
        for (HashMap.Entry<String, Integer> tagsetEntry : tagSet.entrySet()) {
            String tag = tagsetEntry.getKey();

            int acknowledgment = tagsetEntry.getValue();
            if (request.getReceivedBy().getTags().contains(tag)) {
                // if the receiving node has the current tag, reduce the number of acknowledgments
                acknowledgment--;
            }

            // find custom delays of nodes with the current tag
            TreeSet<Long> relevantDelays = findCustomDelaysWithTag(
                tag,
                request.getReceivedBy(),
                nodes
            );

            // calculate delay for this tag
            long tagDelay = calculateObservableReplicationDelay(acknowledgment, relevantDelays);
            if (tagDelay > delay) {
                delay = tagDelay;
            }
        }
        long end = System.currentTimeMillis();
        log.debug("MongoDbAnomalies,calculateTaggedReplicationDelay,{},{}",request.toString(), (end - start));
        return delay;
    }

    /**
     * Returns a sorted list of all delays, that occur from the receiving node
     * to its replicas with a specific tag.
     *
     * @param receivedBy
     * @return a sorted list with all delays
     */
    private TreeSet<Long> findCustomDelaysWithTag(String tag,
                                                  Node receivedBy,
                                                  Set<Node> nodes) {
        // Find all nodes with that tag
        Set<Node> foundNodes = new HashSet<>();
        for (Node node : nodes) {
            if (node.getTags().contains(tag) && node != receivedBy) {
                foundNodes.add(node);
            }
        }

        return findCustomDelays(receivedBy, foundNodes);
    }

    /**
     * Returns a sorted list of all delays, that occur from a request that is propagated to a
     * replica until a confirmation is received.
     *
     * @param receivedBy
     * @param nodes
     * @return a sorted list with all delays
     */
    private TreeSet<Long> findCustomDelays(Node receivedBy, Set<Node> nodes) {
        long start = System.currentTimeMillis();
        TreeSet<Long> delays = new TreeSet<>();

        // calculate the delays that occur by propagating the request to each node
        for (Node node : nodes) {
            if (node == receivedBy) {
                // there is no delay to the primary (the receiving node)
                // so, the node can be ignored
                continue;
            }

            long requestDelay = -1; // delay from primary to replica
            long responseDelay = -1; // delay from replica to primary
            for (NetworkDelay customDelay : customDelays) {
                if (customDelay.getFrom() == receivedBy && customDelay.getTo() == node) {
                    requestDelay = customDelay.getDelay();
                }
                if (customDelay.getTo() == receivedBy && customDelay.getFrom() == node) {
                    responseDelay = customDelay.getDelay();
                }
            }

            if (requestDelay == -1) {
                requestDelay = defaultDelay;
            }
            if (responseDelay == -1) {
                responseDelay = defaultDelay;
            }

            long delay = requestDelay + responseDelay;
            delays.add(delay);
        }
        long end = System.currentTimeMillis();
        log.debug("MongoDbAnomalies,findCustomDelays,{},{}","", (end - start));
        return delays;
    }

    /**
     * Calculates the actual delay that will be produced by the write concern.
     * @param acknowledgments number of acknowledgments that are necessary
     * @param delays a set with all delays until a response is retrieved from a replica
     * @return
     */
    private long calculateObservableReplicationDelay(int acknowledgments, TreeSet<Long> delays) {
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

    public void setTagSets(Map<String, Map<String, Integer>> tagSets) {
        this.tagSets = tagSets;
    }

    public void setTimeHandler(TimeHandler timeHandler) {
        this.timeHandler = timeHandler;
    }
}