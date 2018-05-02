/**
 * 
 */
package de.unihamburg.sickstore.database;

import java.util.List;

import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;

import de.unihamburg.sickstore.SickstoreTestCase;
import de.unihamburg.sickstore.database.client.SickStoreClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;

/**
 * @author Wolfram Wingerath
 * 
 */
public class SickServerTest extends SickstoreTestCase {

    private SickStoreClient c1;
    private SickStoreClient c2;
    private SickStoreClient c3;
    private SickStoreServer server;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // specify connection parameters
        String host = "localhost";
        int timeout = 12000;
        int tcpPort = 54000;

        server = new SickStoreServer(tcpPort, queryHandler);
        server.start();

        // Connect clients
        c1 = new SickStoreClient(host, tcpPort, "node1", timeHandler);
        c2 = new SickStoreClient(host, tcpPort, "node2", timeHandler);
        c3 = new SickStoreClient(host, tcpPort, "node3", timeHandler);
        c1.connectPool();
        c2.connectPool();
        c3.connectPool();
        Thread.sleep(2000);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        c1.disconnect();
        c2.disconnect();
        c3.disconnect();
        server.shutdown();
    }

    private void checkClientStainless(Version insert, SickStoreClient writer,
            String key) throws Exception {
        Version copyC1 = null;
        Version copyC2 = null;
        Version copyC3 = null;

        long start = -1;
        long delayC1 = -1;
        long delayC2 = -1;
        long delayC3 = -1;
        long timeout = 1000;

        writer.insert("", key, insert);

        // Measure time until the write is visible on the other nodes.
        start = timeHandler.getCurrentTime();
        do {
            if (!(copyC1 = c1.read("", key, null)).isNull() && delayC1 == -1) {
                delayC1 = timeHandler.getCurrentTime() - start;
            }
            if (!(copyC2 = c2.read("", key, null)).isNull() && delayC2 == -1) {
                delayC2 = timeHandler.getCurrentTime() - start;
            }
            if (!(copyC3 = c3.read("", key, null)).isNull() && delayC3 == -1) {
                delayC3 = timeHandler.getCurrentTime() - start;
            }

            if (timeHandler instanceof FakeTimeHandler) {
                ((FakeTimeHandler) timeHandler).increaseTime(10);
            }
        // as long as not every item has been read and the timeout is not reached
        } while (timeHandler.getCurrentTime() - start < timeout
                && (delayC1 == -1 || delayC2 == -1 || delayC3 == -1));

        // assert that the data item was read from every node
        assertEquals(insert, copyC1);
        assertEquals(insert, copyC2);
        assertEquals(insert, copyC3);

        System.out.println("writer: " + writer);
        System.out.println("delay client 1:\t" + delayC1);
        System.out.println("delay client 2:\t" + delayC2);
        System.out.println("delay client 3:\t" + delayC3);

        // assert that the writer has a read delay of under 50
        // and that readers have a delay of around 500 (which is expected)
        assertTrue((delayC1 < 50 && c1 == writer) || (450 < delayC1 && delayC1 < 550));
        assertTrue((delayC2 < 50 && c2 == writer) || (450 < delayC2 && delayC2 < 550));
        assertTrue((delayC3 < 50 && c3 == writer) || (450 < delayC3 && delayC3 < 550));
    }

    @Test
    public void testStaleness() throws Exception, DatabaseException {
        // create some data objects
        Version bob = new Version();
        bob.put("name", "bob");
        bob.put("hair", "brown");
        bob.put("age", 25);

        Version john = new Version();
        john.put("name", "john");
        john.put("hair", "yellow");
        john.put("age", 21);

        Version adele = new Version();
        adele.put("name", "adele");
        adele.put("hair", "red");
        adele.put("age", 24);

        Version mike = new Version();
        mike.put("name", "mike");
        mike.put("hair", "blonde");
        mike.put("age", 31);

        checkClientStainless(adele, c1, "adele");
        checkClientStainless(mike, c2, "mike");
        checkClientStainless(john, c3, "john");
        checkClientStainless(bob, c1, "bob");

        List<Version> copies1 = null;
        List<Version> copies2 = null;
        List<Version> copies3 = null;

        // perform range query and compare
        copies2 = c2.scan("", "adele", 3, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(3, copies2.size());

        // extend range and expect one additional row
        copies2 = c2.scan("", "adele", 4, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(mike, copies2.get(3));
        assertEquals(4, copies2.size());

        // a larger range should return the same result
        copies2 = c2.scan("", "adele", 27, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(mike, copies2.get(3));
        assertEquals(4, copies2.size());

        // have the range begin and end somewhere in the middle
        copies2 = c2.scan("", "bob", 2, null);
        assertEquals(bob, copies2.get(0));
        assertEquals(john, copies2.get(1));
        assertEquals(2, copies2.size());

        // remove something and do the same scan again
        assertTrue(c2.delete("", "john"));
        // c2 should see the consequence immediately, c1 and c3 delayed
        copies1 = c1.scan("", "bob", 2, null);
        copies2 = c2.scan("", "bob", 2, null);
        copies3 = c3.scan("", "bob", 2, null);

        assertEquals(2, copies2.size());
        assertEquals(bob, copies2.get(0));
        assertEquals(mike, copies2.get(1));

        // c1 and c3 still sees the old entries
        assertEquals(2, copies1.size());
        assertEquals(bob, copies1.get(0));
        assertEquals(john, copies1.get(1));

        assertEquals(2, copies3.size());
        assertEquals(bob, copies3.get(0));
        assertEquals(john, copies3.get(1));

        timeHandler.sleep(600);

        // c2 and c3 should see the consequence immediately not immediately
        // (after about 500 ms)
        copies1 = c1.scan("", "bob", 2, null);
        copies3 = c3.scan("", "bob", 2, null);
        assertEquals(bob, copies1.get(0));
        assertEquals(mike, copies1.get(1));
        assertEquals(2, copies1.size());

        assertEquals(bob, copies3.get(0));
        assertEquals(mike, copies3.get(1));
        assertEquals(2, copies3.size());
    }
}
