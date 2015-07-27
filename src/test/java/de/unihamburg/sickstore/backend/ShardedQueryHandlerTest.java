package de.unihamburg.sickstore.backend;

import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.sharding.HashBasedStrategy;
import de.unihamburg.sickstore.backend.sharding.ShardingStrategy;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShardedQueryHandlerTest {

    private TimeHandler timeHandler;
    private BasicAnomalyGenerator anomalyGenerator;
    private QueryHandlerInterface queryHandler;

    private Node node1;
    private Node node2;
    private Node node3;

    private Store store1;
    private Store store2;
    private Store store3;

    @Before
    public void initEssentials() throws Exception {
        // create 3 test nodes
        Set<Node> nodes1 = new HashSet<>();
        Set<Node> nodes2 = new HashSet<>();
        Set<Node> nodes3 = new HashSet<>();

        // Each shard gets one node, which is always the primary of the shard
        nodes1.add(node1 = new Node("node1", new HashSet<>(), true));
        nodes2.add(node2 = new Node("node2", new HashSet<>(), true));
        nodes3.add(node3 = new Node("node3", new HashSet<>(), true));

        // init store and query handler
        timeHandler = new FakeTimeHandler();
        anomalyGenerator = new BasicAnomalyGenerator(
            new ConstantStaleness(0, 0),
            new ZeroClientDelay()
        );

        ArrayList<QueryHandlerInterface> shards = new ArrayList<>();
        shards.add(new QueryHandler(store1 = new Store(timeHandler), anomalyGenerator, nodes1, timeHandler));
        shards.add(new QueryHandler(store2 = new Store(timeHandler), anomalyGenerator, nodes2, timeHandler));
        shards.add(new QueryHandler(store3 = new Store(timeHandler), anomalyGenerator, nodes3, timeHandler));

        Map<String, ShardingStrategy> strategies = new HashMap<>();
        strategies.put("", new HashBasedStrategy());

        queryHandler = new ShardedQueryHandler(shards, strategies);
    }

    @Test
    public void testNoShardingStrategy() {
        ServerResponse response;

        Version bob = new Version();
        bob.put("name", "Bob");

        queryHandler.processQuery(new ClientRequestInsert("random", "bob", bob));

        response = queryHandler.processQuery(new ClientRequestRead("random", "bob", null));
        assertEquals(bob, ((ServerResponseRead) response).getVersion());

        // assert it is in store1 (the first one)
        assertEquals(bob, store1.get(node1, "bob", timeHandler.getCurrentTime(), false));
    }

    @Test
    public void testInsertAndRead() {
        ServerResponse response;

        Version bob = new Version();
        bob.put("name", "Bob");

        queryHandler.processQuery(new ClientRequestInsert("", "bob", bob));
        response = queryHandler.processQuery(new ClientRequestRead("", "bob", null));
        assertEquals(bob, ((ServerResponseRead) response).getVersion());

        assertStoredOnce("bob");
    }

    @Test
    public void testScan() {
        ServerResponse response;

        Version bob = new Version();
        bob.put("name", "Bob");
        Version bobi = new Version();
        bobi.put("name", "Bobi");
        Version bobo = new Version();
        bobo.put("name", "Bobo");

        queryHandler.processQuery(new ClientRequestInsert("", "bob", bob));
        queryHandler.processQuery(new ClientRequestInsert("", "bobi", bobi));
        queryHandler.processQuery(new ClientRequestInsert("", "bobo", bobo));

        assertStoredOnce("bob");
        assertStoredOnce("bobi");
        assertStoredOnce("bobo");

        response = queryHandler.processQuery(new ClientRequestScan("", "bob", 2, null, true));
        List<Version> versions = ((ServerResponseScan) response).getEntries();

        assertEquals(2, versions.size());
        assertEquals(bob, versions.get(0));
        assertEquals(bobi, versions.get(1));
    }

    /**
     * Assert the key was stored in only one store.
     *
     * @return
     */
    private void assertStoredOnce(String key)
    {
        int stored = 0;

        Version storeCheck1 = store1.get(node1, key, timeHandler.getCurrentTime(), false);
        Version storeCheck2 = store2.get(node2, key, timeHandler.getCurrentTime(), false);
        Version storeCheck3 = store3.get(node3, key, timeHandler.getCurrentTime(), false);

        if (!storeCheck1.isNull()) {
            stored++;
        }
        if (!storeCheck2.isNull()) {
            stored++;
        }
        if (!storeCheck3.isNull()) {
            stored++;
        }

        assertEquals(1, stored);
    }
}
