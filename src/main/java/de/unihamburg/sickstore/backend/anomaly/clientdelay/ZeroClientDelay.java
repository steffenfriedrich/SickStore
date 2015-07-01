package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.Set;

public class ZeroClientDelay implements ClientDelayGenerator {

    @Override
    public long calculateDelay(ClientRequest request, Set<Node> nodes) {
        return 0;
    }
}
