package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.Map;
import java.util.Set;

public interface AnomalyGenerator {

    /**
     * Calculates the amount of time after that a write becomes visible to a specific server.
     *
     * @param request       the incoming request
     * @param nodes       set of all server ids
     * @return map of ServerId to Delay (in ms)
     */
    StalenessMap getWriteVisibility(ClientRequest request, Set<Node> nodes);

    /**
     * Modify the response object to reflect anomalies.
     *
     * @param response
     * @param request
     */
    void handleResponse(ServerResponse response, ClientRequest request, Set<Node> nodes);
}
