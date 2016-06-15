package de.unihamburg.sickstore.backend.anomaly;

import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.database.Node;

public class Anomaly {

    private long clientDelay = 0;
    private StalenessMap stalenessMap = new StalenessMap();
    private Node responsiveNode = new Node();

    public long getClientDelay() {
        return clientDelay;
    }

    public void setClientDelay(long clientDelay) {
        this.clientDelay = clientDelay;
    }

    public StalenessMap getStalenessMap() {
        return stalenessMap;
    }

    public void setStalenessMap(StalenessMap stalenessMap) {
        this.stalenessMap = stalenessMap;
    }

    public Node getResponsiveNode() {
        return responsiveNode;
    }

    public void setResponsiveNode(Node responsiveNode) {
        this.responsiveNode = responsiveNode;
    }
}
