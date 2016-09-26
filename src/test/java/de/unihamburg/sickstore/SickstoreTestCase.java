package de.unihamburg.sickstore;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import org.junit.Before;

import java.util.HashSet;
import java.util.Set;

public abstract class SickstoreTestCase {

    protected TimeHandler timeHandler;
    protected BasicAnomalyGenerator anomalyGenerator;
    protected QueryHandlerInterface queryHandler;

    protected Node node1;
    protected Node node2;
    protected Node node3;

    @Before
    public void initEssentials() throws Exception {
        // create 3 test nodes
        Set<Node> nodes = new HashSet<>();
        nodes.add(node1 = new Node("node1"));
        nodes.add(node2 = new Node("node2"));
        nodes.add(node3 = new Node("node3"));

        // init store and query handler
        timeHandler = new FakeTimeHandler();
        anomalyGenerator = new BasicAnomalyGenerator(
            new ConstantStaleness(500, 0),
            new ZeroClientDelay()
        );

        Store store = new Store(timeHandler);
        queryHandler = new QueryHandler(store, anomalyGenerator, nodes, timeHandler, 0, false, false);
    }
}
