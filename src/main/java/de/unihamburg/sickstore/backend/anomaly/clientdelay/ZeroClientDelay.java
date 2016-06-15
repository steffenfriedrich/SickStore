package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.anomaly.Anomaly;
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
    public long calculateDelay(ClientRequest request, Set<Node> nodes, Anomaly anomaly) {
        return 0;
    }

    @Override
    public Node getResponsiveNode(ClientRequest request, Set<Node> nodes) {
        return request.getReceivedBy();
    }


}
