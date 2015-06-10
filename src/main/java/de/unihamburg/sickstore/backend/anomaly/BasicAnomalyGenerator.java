package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BasicAnomalyGenerator implements AnomalyGenerator {

    private StalenessGenerator stalenessGenerator;
    private ClientDelayGenerator clientDelayGenerator;

    public BasicAnomalyGenerator(StalenessGenerator stalenessGenerator, ClientDelayGenerator clientDelayGenerator) {
        this.stalenessGenerator = stalenessGenerator;
        this.clientDelayGenerator = clientDelayGenerator;
    }

    @Override
    public Map<Integer, Long> getWriteVisibility(ClientRequest request, Set<Integer> servers) {
        if (stalenessGenerator == null) {
            return new HashMap<>();
        }

        return stalenessGenerator.get(servers, request);
    }

    @Override
    public void handleResponse(ServerResponse response, ClientRequest request, Set<Integer> servers) {
        if (request.isWriteRequest() && clientDelayGenerator != null) {
            long delay = clientDelayGenerator.calculateDelay(servers, request);
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
