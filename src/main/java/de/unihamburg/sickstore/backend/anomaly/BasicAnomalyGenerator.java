package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.ClientDelayGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessGenerator;
import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestWrite;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class BasicAnomalyGenerator implements AnomalyGenerator {
    private static final Logger log = LoggerFactory.getLogger("sickstore");
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
        Map<String, Object> combinedGeneratorConfig = (Map<String, Object>) config.get("combinedGenerator");

        StalenessGenerator stalenessGenerator;
        ClientDelayGenerator clientDelayGenerator;

        if (combinedGeneratorConfig != null) {
            if (config.containsKey("nodes")) {
                combinedGeneratorConfig.put("nodes", config.get("nodes"));
            }

            // a combined generator implements all types of anomalies
            Object generator = InstanceFactory.newInstanceFromConfig(combinedGeneratorConfig);

            stalenessGenerator = (StalenessGenerator) generator;
            clientDelayGenerator = (ClientDelayGenerator) generator;
        } else {
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

            stalenessGenerator = (StalenessGenerator) InstanceFactory
                .newInstanceFromConfig(stalenessGeneratorConfig);
            clientDelayGenerator = (ClientDelayGenerator) InstanceFactory
                .newInstanceFromConfig(clientDelayGeneratorConfig);
        }

        return new BasicAnomalyGenerator(stalenessGenerator, clientDelayGenerator);
    }

    public BasicAnomalyGenerator() {
        this.stalenessGenerator = new ConstantStaleness(0, 0);
        this.clientDelayGenerator = new ZeroClientDelay();
    }

    public BasicAnomalyGenerator(StalenessGenerator stalenessGenerator,
                                 ClientDelayGenerator clientDelayGenerator) {
        this.stalenessGenerator = stalenessGenerator;
        this.clientDelayGenerator = clientDelayGenerator;
    }

    @Override
    public Anomaly handleRequest(ClientRequest request, Set<Node> nodes) {
        Anomaly anomaly = new Anomaly();

        if (request instanceof ClientRequestWrite && stalenessGenerator != null) {
            anomaly.setStalenessMap(stalenessGenerator.generateStalenessMap(nodes, request));
        }
        return anomaly;
    }

    @Override
    public void handleResponse(Anomaly anomaly, ClientRequest request, ServerResponse response,
                               Set<Node> nodes) {
        long delay = clientDelayGenerator.calculateDelay(request, nodes);
        anomaly.setClientDelay(delay);
        response.setWaitTimeout(delay);
        response.setSentByClientAt(request.getSendedByClientAt());
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
