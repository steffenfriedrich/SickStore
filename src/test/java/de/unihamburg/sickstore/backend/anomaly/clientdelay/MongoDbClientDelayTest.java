package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.Node;
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

        Set<Node> servers = new HashSet<>();
        ClientRequest request = new ClientRequestInsert("", "example", new Version());

        // no other servers -> delay is 0
        assertEquals(0, delayGenerator.calculateDelay(servers, request));

        // another other server, but min acknowledgement is 0 -> delay is 0
        servers.add(new Node(0));
        assertEquals(0, delayGenerator.calculateDelay(servers, request));

        // another other server, min acknowledgement = 1 -> delay is default delay (42)
        delayGenerator.setMinAcknowledgements(1);
        assertEquals(0, delayGenerator.calculateDelay(servers, request));
    }

    @Test
    public void testCustomDelays() {
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        Node node3 = new Node(3);
        Node node4 = new Node(4);

        Map<Node[], Long> customDelays = new HashMap<>();
        customDelays.put(new Node[] {node1, node2}, 100l); // 100 ms delay from server 1 to server 2

        MongoDbClientDelay delayGenerator = new MongoDbClientDelay(42l, 0, customDelays);

        Set<Node> servers = new HashSet<>();
        servers.add(node1);
        servers.add(node2);
        servers.add(node3);
        servers.add(node4);

        ClientRequest request = new ClientRequestInsert("", "example", new Version());
        request.setReceivedBy(1);

        // no min acknowledgements -> delay is 0
        assertEquals(0, delayGenerator.calculateDelay(servers, request));

        // with min Acknowledgement -> delay is 100
        delayGenerator.setMinAcknowledgements(1);
        assertEquals(100, delayGenerator.calculateDelay(servers, request));

        // add more custom delays. We want to find the shortest delay which fulfills the min acknowledgements
        delayGenerator.setMinAcknowledgements(2);
        customDelays.put(new Node[] {node2, node2}, 0l); // wrong direction, must be ignored
        customDelays.put(new Node[] {node2, node3}, 0l); // wrong direction, must be ignored
        customDelays.put(new Node[] {node1, node3}, 10l);
        customDelays.put(new Node[] {node1, node4}, 50l);

        // the custom delays from 1 to 3 and 4 can fulfill the min acknowledgement of 2 in 50 ms
        assertEquals(50, delayGenerator.calculateDelay(servers, request));

        // more min acknowledgements than custom delays
        // but the highest custom delay is higher than the default delay, so we expect 100
        delayGenerator.setMinAcknowledgements(10);
        assertEquals(100, delayGenerator.calculateDelay(servers, request));

        // default delay now higher than the highest custom delay, we expect that
        delayGenerator.setDefaultDelay(1000l);
        assertEquals(1000, delayGenerator.calculateDelay(servers, request));
    }
}
