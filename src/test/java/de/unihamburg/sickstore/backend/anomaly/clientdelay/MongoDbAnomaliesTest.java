package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.anomaly.MongoDbAnomalies;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestInsert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class MongoDbAnomaliesTest {

    private TimeHandler timeHandler;
    private MongoDbAnomalies mongoDbAnomalies;

    @Before
    public void setUp()
    {
        timeHandler = new FakeTimeHandler();

        mongoDbAnomalies = new MongoDbAnomalies(
            42l,
            300,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            timeHandler
        );
    }

    @Test
    public void testDefaultDelay() {
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node());

        WriteConcern writeConcern = new WriteConcern();
        ClientRequest request = new ClientRequestInsert("", "example", new Version(), writeConcern, "");

        // no other nodes -> delay is 0
        assertEquals(0, mongoDbAnomalies.calculateDelay(request, nodes));

        // another node, but min acknowledgement is 1 -> delay is 0
        nodes.add(new Node("name"));
        assertEquals(0, mongoDbAnomalies.calculateDelay(request, nodes));

        // another node, min acknowledgement = 2
        // delay is based on default delay (42 + 42, request to + response from replica)
        writeConcern.setReplicaAcknowledgement(2);
        assertEquals(84, mongoDbAnomalies.calculateDelay(request, nodes));
    }

    @Test
    public void testCustomDelays() {
        Node node1 = new Node("1");
        Node node2 = new Node("2");
        Node node3 = new Node("3");
        Node node4 = new Node("4");

        Set<NetworkDelay> customDelays = new HashSet<>();
        customDelays.add(new NetworkDelay(node1, node2, 50l)); // 100 ms delay from node 1 to node 2
        customDelays.add(new NetworkDelay(node2, node1, 25l)); // 50 ms delay from node 2 to node 1

        mongoDbAnomalies.setCustomDelays(customDelays);

        Set<Node> nodes = new HashSet<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);

        WriteConcern writeConcern = new WriteConcern();
        ClientRequest request = new ClientRequestInsert(
            "", "example", new Version(), writeConcern, node1.getName()
        );
        request.setReceivedBy(node1);

        // no min acknowledgements -> delay is 0
        writeConcern.setReplicaAcknowledgement(0);
        assertEquals(0, mongoDbAnomalies.calculateDelay(request, nodes));

        // acknowledgement of the primary -> delay is 0
        writeConcern.setReplicaAcknowledgement(1);
        assertEquals(0, mongoDbAnomalies.calculateDelay(request, nodes));

        // with acknowledgement of additional node -> delay is 150 (primary to replica and back)
        writeConcern.setReplicaAcknowledgement(2);
        assertEquals(75, mongoDbAnomalies.calculateDelay(request, nodes));

        // add more custom delays
        // we want to find the shortest delay which fulfills the min acknowledgements
        writeConcern.setReplicaAcknowledgement(3);
        customDelays.add(new NetworkDelay(node2, node3, 0l)); // does not match -> is ignored
        customDelays.add(new NetworkDelay(node1, node3, 10l)); // replication to node 3 takes 10 + 42 (default delay) = 52 ms
        customDelays.add(new NetworkDelay(node1, node4, 50l));
        customDelays.add(new NetworkDelay(node4, node1, 10l)); // replication to node 4 takes 50 + 10 = 60 ms

        assertEquals(60, mongoDbAnomalies.calculateDelay(request, nodes));

        // more min acknowledgements than custom delays
        // but the highest custom delay is higher than the default delay, so we expect 75
        writeConcern.setReplicaAcknowledgement(10);
        assertEquals(75, mongoDbAnomalies.calculateDelay(request, nodes));

        // no custom delay for response from node 3 to node 1, so the default delay will be used
        // 10 + 1000 = 1010 is expected
        mongoDbAnomalies.setDefaultDelay(1000l);
        assertEquals(1010, mongoDbAnomalies.calculateDelay(request, nodes));
    }

    @Test
    public void testJournaling() {
        WriteConcern writeConcern = new WriteConcern(0, true, 0);
        ClientRequest request = new ClientRequestInsert("", "example", new Version(), writeConcern);

        long delay = mongoDbAnomalies.calculateDelay(request, new HashSet<>());
        assertEquals(100, delay);

        timeHandler.sleep(50);
        delay = mongoDbAnomalies.calculateDelay(request, new HashSet<>());
        assertEquals(50, delay);

        timeHandler.sleep(49);
        delay = mongoDbAnomalies.calculateDelay(request, new HashSet<>());
        assertEquals(1, delay);

        // commit is running, we need to wait for the next one
        timeHandler.sleep(1);
        delay = mongoDbAnomalies.calculateDelay(request, new HashSet<>());
        assertEquals(100, delay);

        // change journal commit interval
        // current time is 100 -> next commit in 32
        mongoDbAnomalies.setJournalCommitInterval(100);
        delay = mongoDbAnomalies.calculateDelay(request, new HashSet<>());
        assertEquals(32, delay);
    }

    @Test
    public void testTaggedWriteConcern() {
        // create nodes
        Node primary = new Node("Primary", new HashSet<>(Arrays.asList("A")), true);
        Node node1 = new Node("Replica1", new HashSet<>(Arrays.asList("A", "B")));
        Node node2 = new Node("Replica2", new HashSet<>(Arrays.asList("B")));
        Node node3 = new Node("Replica3", new HashSet<>(Arrays.asList("B")));
        Node node4 = new Node("Replica4");

        Set<Node> nodes = new HashSet<>();
        nodes.add(primary);
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);

        // create tagset
        HashMap<String, Integer> tagConcern = new HashMap<>();
        tagConcern.put("A", 1);
        tagConcern.put("B", 2);

        Map<String, Map<String, Integer>> tagSets = new HashMap<>();
        tagSets.put("tagConcern", tagConcern);
        mongoDbAnomalies.setTagSets(tagSets);

        // create custom delays
        Set<NetworkDelay> customDelays = new HashSet<>();
        customDelays.add(new NetworkDelay(primary, node1, 10l));
        customDelays.add(new NetworkDelay(primary, node2, 20l));
        customDelays.add(new NetworkDelay(primary, node3, 5l));
        customDelays.add(new NetworkDelay(primary, node4, 10l));
        mongoDbAnomalies.setCustomDelays(customDelays);

        WriteConcern writeConcern = new WriteConcern("tagConcern");
        ClientRequest request = new ClientRequestInsert("", "example", new Version(), writeConcern, "Primary");
        request.setReceivedBy(primary);

        long delay = mongoDbAnomalies.calculateDelay(request, nodes);
        assertEquals(52, delay);

        // expect three acks for B
        tagConcern.put("B", 3);
        delay = mongoDbAnomalies.calculateDelay(request, nodes);
        assertEquals(62, delay);

        // no custom delays, expect the default
        customDelays.clear();
        delay = mongoDbAnomalies.calculateDelay(request, nodes);
        assertEquals(84, delay);
    }

    @Test
    public void testStalenessGenerator() {
        // create nodes
        Node primary = new Node("Primary");
        Node node1 = new Node("Replica1");
        Node node2 = new Node("Replica2");
        Node node3 = new Node("Replica3");
        Node node4 = new Node("Replica4");

        Set<Node> nodes = new HashSet<>();
        nodes.add(primary);
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);

        // create custom delays
        Set<NetworkDelay> customDelays = new HashSet<>();
        customDelays.add(new NetworkDelay(primary, node1, 10l));
        customDelays.add(new NetworkDelay(primary, node2, 20l));
        customDelays.add(new NetworkDelay(primary, node3, 5l));
        customDelays.add(new NetworkDelay(node3, primary, 5l)); // the inverse side has no effect
        mongoDbAnomalies.setCustomDelays(customDelays);

        ClientRequest request = new ClientRequestInsert("", "example", new Version(), "Primary");
        request.setReceivedBy(primary);

        StalenessMap stalenessMap = mongoDbAnomalies.generateStalenessMap(nodes, request);
        assertEquals(0, (long) stalenessMap.get(primary)); // the primary has no staleness
        assertEquals(42, (long) stalenessMap.get(node4)); // default delay

        // the following staleness values correspond to the configured delay
        assertEquals(10, (long) stalenessMap.get(node1));
        assertEquals(20, (long) stalenessMap.get(node2));
        assertEquals(5, (long) stalenessMap.get(node3));
    }
}
