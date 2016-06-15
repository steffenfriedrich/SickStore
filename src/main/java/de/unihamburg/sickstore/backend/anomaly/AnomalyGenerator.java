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
     * @return
     */
    Anomaly handleRequest(ClientRequest request, Set<Node> nodes);

    /**
     * Handle the response for a request. The anomalies generated from the request can be updated
     * here, depending on the result.
     *  @param anomaly the anomalies generated for the request
     * @param request
     * @param response
     */
    void handleResponse(Anomaly anomaly,
                        ClientRequest request,
                        ServerResponse response,
                        Set<Node> nodes);
}
