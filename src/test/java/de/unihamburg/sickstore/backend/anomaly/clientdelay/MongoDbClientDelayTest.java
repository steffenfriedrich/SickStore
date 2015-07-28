package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.Version;
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

public class MongoDbClientDelayTest {

    private TimeHandler timeHandler;
    private MongoDbClientDelay delayGenerator;

    @Before
    public void setUp()
    {
        timeHandler = new FakeTimeHandler();

        delayGenerator = new MongoDbClientDelay(
            42l,
            300,
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
        assertEquals(0, delayGenerator.calculateDelay(request, nodes));

        // another node, but min acknowledgement is 1 -> delay is 0
        nodes.add(new Node("name"));
        assertEquals(0, delayGenerator.calculateDelay(request, nodes));

        // another node, min acknowledgement = 2 -> delay is default delay (42)
        writeConcern.setReplicaAcknowledgement(2);
        assertEquals(42, delayGenerator.calculateDelay(request, nodes));
    }

    @Test
    public void testCustomDelays() {
        Node node1 = new Node("1");
        Node node2 = new Node("2");
        Node node3 = new Node("3");
        Node node4 = new Node("4");

        Set<NetworkDelay> customDelays = new HashSet<>();
        customDelays.add(new NetworkDelay(node1, node2, 100l)); // 100 ms delay from node 1 to node 2

        delayGenerator.setCustomDelays(customDelays);

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
        assertEquals(0, delayGenerator.calculateDelay(request, nodes));

        // acknowledgement of the primary -> delay is 0
        writeConcern.setReplicaAcknowledgement(1);
        assertEquals(0, delayGenerator.calculateDelay(request, nodes));

        // with min Acknowledgement of one other node -> delay is 100
        writeConcern.setReplicaAcknowledgement(2);
        assertEquals(100, delayGenerator.calculateDelay(request, nodes));

        // add more custom delays. We want to find the shortest delay which fulfills the min acknowledgements
        writeConcern.setReplicaAcknowledgement(3);
        customDelays.add(new NetworkDelay(node2, node2, 0l)); // wrong direction, must be ignored
        customDelays.add(new NetworkDelay(node2, node3, 0l)); // wrong direction, must be ignored
        customDelays.add(new NetworkDelay(node1, node3, 10l));
        customDelays.add(new NetworkDelay(node1, node4, 50l));

        // the custom delays from 1 to 3 and 4 can fulfill the min acknowledgement of 3 in 50 ms
        assertEquals(50, delayGenerator.calculateDelay(request, nodes));

        // more min acknowledgements than custom delays
        // but the highest custom delay is higher than the default delay, so we expect 100
        writeConcern.setReplicaAcknowledgement(10);
        assertEquals(100, delayGenerator.calculateDelay(request, nodes));

        // default delay now higher than the highest custom delay, we expect that
        delayGenerator.setDefaultDelay(1000l);
        assertEquals(1000, delayGenerator.calculateDelay(request, nodes));
    }

    @Test
    public void testJournaling() {
        WriteConcern writeConcern = new WriteConcern(0, true, 0);
        ClientRequest request = new ClientRequestInsert("", "example", new Version(), writeConcern);

        long delay = delayGenerator.calculateDelay(request, new HashSet<>());
        assertEquals(100, delay);

        timeHandler.sleep(50);
        delay = delayGenerator.calculateDelay(request, new HashSet<>());
        assertEquals(50, delay);

        timeHandler.sleep(49);
        delay = delayGenerator.calculateDelay(request, new HashSet<>());
        assertEquals(1, delay);

        // commit is running, we need to wait for the next one
        timeHandler.sleep(1);
        delay = delayGenerator.calculateDelay(request, new HashSet<>());
        assertEquals(100, delay);

        // change journal commit interval
        // current time is 100 -> next commit in 32
        delayGenerator.setJournalCommitInterval(100);
        delay = delayGenerator.calculateDelay(request, new HashSet<>());
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
        delayGenerator.setTagSets(tagSets);

        // create custom delays
        Set<NetworkDelay> customDelays = new HashSet<>();
        customDelays.add(new NetworkDelay(primary, node1, 10l));
        customDelays.add(new NetworkDelay(primary, node2, 20l));
        customDelays.add(new NetworkDelay(primary, node3, 5l));
        customDelays.add(new NetworkDelay(primary, node4, 10l));
        delayGenerator.setCustomDelays(customDelays);

        WriteConcern writeConcern = new WriteConcern("tagConcern");
        ClientRequest request = new ClientRequestInsert("", "example", new Version(), writeConcern, "Primary");
        request.setReceivedBy(primary);

        long delay = delayGenerator.calculateDelay(request, nodes);
        assertEquals(10, delay);

        // expect three acks for B
        tagConcern.put("B", 3);
        delay = delayGenerator.calculateDelay(request, nodes);
        assertEquals(20, delay);

        // no custom delays, expect the default
        customDelays.clear();
        delay = delayGenerator.calculateDelay(request, nodes);
        assertEquals(42, delay);
    }
}
