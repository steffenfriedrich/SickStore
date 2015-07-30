package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.database.messages.ClientRequestRead;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RangeBasedStrategyTest {

    private ShardingStrategy strategy;
    private List<QueryHandlerInterface> shards;

    @Before
    public void before() {
        shards = new ArrayList<>();
        shards.add(new QueryHandler(new Store(), new BasicAnomalyGenerator(), new HashSet<>()));
        shards.add(new QueryHandler(new Store(), new BasicAnomalyGenerator(), new HashSet<>()));
        shards.add(new QueryHandler(new Store(), new BasicAnomalyGenerator(), new HashSet<>()));
        shards.add(new QueryHandler(new Store(), new BasicAnomalyGenerator(), new HashSet<>()));
    }

    @Test
    public void testStrategy() {
        strategy = new RangeBasedStrategy(new String[] { "A", "D", "Udo" });

        testShardNumber("!32", 0);
        testShardNumber("032", 0);
        testShardNumber("Adam", 1);
        testShardNumber("Bob", 1);
        testShardNumber("Cdefghijklmn", 1); // string length is irrelevant, everything is before D
        testShardNumber("D", 1);
        testShardNumber("Dora", 2);
        testShardNumber("Udn", 2);
        testShardNumber("Udp", 3);
        testShardNumber("adam", 3); // case sensitive, a comes later
    }

    @Test
    public void testCaseInsensitiveStrategy() {
        strategy = new RangeBasedStrategy(new String[] { "A", "D", "Udo" }, false);

        testShardNumber("!32", 0);
        testShardNumber("032", 0);
        testShardNumber("Adam", 1);
        testShardNumber("adam", 1); // case insensitive now
        testShardNumber("Bob", 1);
        testShardNumber("D", 1);
        testShardNumber("Dora", 2);
        testShardNumber("Udn", 2);
        testShardNumber("Udp", 3);
    }

    /**
     * Tests whether the detected shard is the expected one.
     */
    private void testShardNumber(String key, int expected) {
        QueryHandlerInterface shard = strategy.getTargetShard(
            new ClientRequestRead("", key, null),
            shards
        );

        String message = "Expected shard " + expected + " but got shard " + shards.indexOf(shard);
        assertSame(message, shard, shards.get(expected));
    }
}
