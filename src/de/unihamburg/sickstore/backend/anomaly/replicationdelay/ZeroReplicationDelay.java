package de.unihamburg.sickstore.backend.anomaly.replicationdelay;

import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.Set;

public class ZeroReplicationDelay implements ReplicationDelayGenerator {

    @Override
    public long calculateDelay(Set<Integer> servers, ClientRequest request) {
        return 0;
    }
}
