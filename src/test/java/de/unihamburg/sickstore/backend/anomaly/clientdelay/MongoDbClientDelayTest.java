package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestInsert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MongoDbClientDelayTest {

    @Test
    public void testDefaultDelay() {
        MongoDbClientDelay delayGenerator = new MongoDbClientDelay(42l);

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node());

        WriteConcern writeConcern = new WriteConcern();
        ClientRequest request = new ClientRequestInsert("", "", "example", new Version(), writeConcern);

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

        Map<Node[], Long> customDelays = new HashMap<>();
        customDelays.put(new Node[] {node1, node2}, 100l); // 100 ms delay from node 1 to node 2

        MongoDbClientDelay delayGenerator = new MongoDbClientDelay(42l, customDelays);

        Set<Node> nodes = new HashSet<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);

        WriteConcern writeConcern = new WriteConcern();
        ClientRequest request = new ClientRequestInsert(
            node1.getName(),
            "",
            "example",
            new Version(),
            writeConcern
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
        customDelays.put(new Node[]{node2, node2}, 0l); // wrong direction, must be ignored
        customDelays.put(new Node[]{node2, node3}, 0l); // wrong direction, must be ignored
        customDelays.put(new Node[]{node1, node3}, 10l);
        customDelays.put(new Node[]{node1, node4}, 50l);

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
}
