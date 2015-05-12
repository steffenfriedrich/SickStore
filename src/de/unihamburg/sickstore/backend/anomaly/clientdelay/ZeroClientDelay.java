package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.Set;

public class ZeroClientDelay implements ClientDelayGenerator {

    @Override
    public long calculateDelay(Set<Integer> servers, ClientRequest request) {
        return 0;
    }
}
