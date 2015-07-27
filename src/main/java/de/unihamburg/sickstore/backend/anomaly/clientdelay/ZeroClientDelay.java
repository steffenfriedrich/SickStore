package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.Map;
import java.util.Set;

public class ZeroClientDelay implements ClientDelayGenerator {

    @SuppressWarnings("unused")
    public static ZeroClientDelay newInstanceFromConfig(Map<String, Object> config) {
        return new ZeroClientDelay();
    }

    @Override
    public long calculateDelay(ClientRequest request, Set<Node> nodes) {
        return 0;
    }
}
