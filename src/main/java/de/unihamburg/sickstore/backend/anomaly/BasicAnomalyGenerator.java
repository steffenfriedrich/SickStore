package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.Set;

public class BasicAnomalyGenerator implements AnomalyGenerator {

    private StalenessGenerator stalenessGenerator;
    private ClientDelayGenerator clientDelayGenerator;

    public BasicAnomalyGenerator(StalenessGenerator stalenessGenerator, ClientDelayGenerator clientDelayGenerator) {
        this.stalenessGenerator = stalenessGenerator;
        this.clientDelayGenerator = clientDelayGenerator;
    }

    @Override
    public StalenessMap getWriteVisibility(ClientRequest request, Set<Node> nodes) {
        if (stalenessGenerator == null) {
            return new StalenessMap();
        }

        return stalenessGenerator.get(nodes, request);
    }

    @Override
    public void handleResponse(ServerResponse response, ClientRequest request, Set<Node> nodes) {
        if (request.isWriteRequest() && clientDelayGenerator != null) {
            long delay = clientDelayGenerator.calculateDelay(request, nodes);
            response.setWaitTimeout(delay);
        }
    }

    public StalenessGenerator getStalenessGenerator() {
        return stalenessGenerator;
    }

    public void setStalenessGenerator(StalenessGenerator stalenessGenerator) {
        this.stalenessGenerator = stalenessGenerator;
    }

    public ClientDelayGenerator getClientDelayGenerator() {
        return clientDelayGenerator;
    }

    public void setClientDelayGenerator(ClientDelayGenerator clientDelayGenerator) {
        this.clientDelayGenerator = clientDelayGenerator;
    }
}
