package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientWriteRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.Map;
import java.util.Set;

public class BasicAnomalyGenerator implements AnomalyGenerator {

    private StalenessGenerator stalenessGenerator;
    private ClientDelayGenerator clientDelayGenerator;

    /**
     * Creates a new instance of this class from the passed config (which is read from a yaml file).
     *
     * @param config
     * @return
     */
    @SuppressWarnings("unused")
    public static BasicAnomalyGenerator newInstanceFromConfig(Map<String, Object> config) {
        Map<String, Object> stalenessGeneratorConfig = (Map<String, Object>) config.get("stalenessGenerator");
        Map<String, Object> clientDelayGeneratorConfig = (Map<String, Object>) config.get("clientDelayGenerator");
        if (stalenessGeneratorConfig == null) {
            throw new RuntimeException("Missing staleness generator");
        }
        if (clientDelayGeneratorConfig == null) {
            throw new RuntimeException("Missing client delay generator");
        }

        if (config.containsKey("nodes")) {
            stalenessGeneratorConfig.put("nodes", config.get("nodes"));
            clientDelayGeneratorConfig.put("nodes", config.get("nodes"));
        }

        StalenessGenerator stalenessGenerator = (StalenessGenerator) InstanceFactory
            .newInstanceFromConfig(stalenessGeneratorConfig);
        ClientDelayGenerator clientDelayGenerator = (ClientDelayGenerator) InstanceFactory
            .newInstanceFromConfig(clientDelayGeneratorConfig);

        return new BasicAnomalyGenerator(stalenessGenerator, clientDelayGenerator);
    }

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
