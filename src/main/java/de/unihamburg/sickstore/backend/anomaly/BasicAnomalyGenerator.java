package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientWriteRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.Set;

public class BasicAnomalyGenerator implements AnomalyGenerator {

    private StalenessGenerator stalenessGenerator;
    private ClientDelayGenerator clientDelayGenerator;

    public BasicAnomalyGenerator(StalenessGenerator stalenessGenerator,
                                 ClientDelayGenerator clientDelayGenerator) {
        this.stalenessGenerator = stalenessGenerator;
        this.clientDelayGenerator = clientDelayGenerator;
    }

    @Override
    public Anomaly handleRequest(ClientRequest request, Set<Node> nodes) {
        Anomaly anomaly = new Anomaly();

        if (request instanceof ClientWriteRequest && stalenessGenerator != null) {
            anomaly.setStalenessMap(stalenessGenerator.get(nodes, request));
        }

        return anomaly;
    }

    @Override
    public void handleResponse(Anomaly anomaly, ClientRequest request, ServerResponse response,
                               Set<Node> nodes) {
        if (request instanceof ClientWriteRequest && clientDelayGenerator != null) {
            long delay = clientDelayGenerator.calculateDelay(request, nodes);

            anomaly.setClientDelay(delay);
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
