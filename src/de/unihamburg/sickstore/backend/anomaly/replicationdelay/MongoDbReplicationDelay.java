package de.unihamburg.sickstore.backend.anomaly.replicationdelay;

import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.*;

public class MongoDbReplicationDelay implements ReplicationDelayGenerator {

    /** time needed to contact a replica until the response arrives */
    private long defaultDelay;

    /** number of nodes which need to confirm the write */
    private int minAcknowledgements;

    /** custom delays between two servers */
    private Map<Integer[], Long> customDelays = new HashMap<>();

    /**
     *
     * @param defaultDelay time needed (in ms) to send data to a replica until the
     *                     response arrives
     */
    public MongoDbReplicationDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    /**
     *
     * @param defaultDelay time needed (in ms) to send data to a replica until the
     *                     response arrives
     */
    public MongoDbReplicationDelay(long defaultDelay,
                                   int minAcknowledgements,
                                   Map<Integer[], Long> customDelays) {
        this.defaultDelay = defaultDelay;
        this.minAcknowledgements = minAcknowledgements;
        this.customDelays = customDelays;
    }

    /**
     * @see ReplicationDelayGenerator#calculateDelay(Set, ClientRequest)
     */
    @Override
    public long calculateDelay(Set<Integer> servers, ClientRequest request) {
        if (servers.size() == 1 || minAcknowledgements == 0) {
            // if there is only one server or it should not be waited for replica acknowledgements
            // there delay is zero
            return 0;
        }

        if (customDelays.size() == 0) {
            return defaultDelay;
        }

        // check whether there are custom delays
        Set<Long> delays = new TreeSet<>();
        long delay = 0;

        for (Map.Entry<Integer[], Long> customDelay : customDelays.entrySet()) {
            Integer[] delayBetween = customDelay.getKey();
            if (delayBetween[0] == request.getReceivedBy()) {
                // there is custom delay between the server which received the request
                // and another server

                delays.add(customDelay.getValue());
            }
        }

        Iterator<Long> it = delays.iterator();
        int acknowledgementsLeft = minAcknowledgements;
        while (it.hasNext() && acknowledgementsLeft > 0) {
            Long tmpDelay = it.next();
            if (tmpDelay > delay) {
                delay = tmpDelay;
            }

            acknowledgementsLeft--;
        }

        if (minAcknowledgements > 0 && delay < defaultDelay) {
            delay = defaultDelay;
        }

        return delay;
    }

    public void setDefaultDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    public void setMinAcknowledgements(int minAcknowledgements) {
        this.minAcknowledgements = minAcknowledgements;
    }

    public void setCustomDelays(Map<Integer[], Long> customDelays) {
        this.customDelays = customDelays;
    }
}