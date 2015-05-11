package de.unihamburg.sickstore.backend;

import java.util.List;
import java.util.Set;

import de.unihamburg.sickstore.backend.anomaly.replicationdelay.MongoDbReplicationDelay;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.test.SickstoreTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.unihamburg.sickstore.database.SickServer;

public class QueryHandlerTest extends SickstoreTestCase {

    private SickServer server1;
    private SickServer server3;
    private SickServer server2;

    @Before
    public void setUp() throws Exception {
        // specify connection parameters
        int tcpPort = 54000;

        // Create and start server and clients
        server1 = new SickServer(tcpPort + 1, queryHandler, timeHandler);
        server2 = new SickServer(tcpPort + 2, queryHandler, timeHandler);
        server3 = new SickServer(tcpPort + 3, queryHandler, timeHandler);
    }

    @After
    public void tearDown() throws Exception {
        server1.shutdown();
        server2.shutdown();
        server3.shutdown();
        store.clear();
    }

    /**
     * Inserts a data item on a specific server and measures the time until
     * it becomes visible for the other servers.
     *
     * @param insert
     * @param writer
     * @param key
     * @throws Exception
     */
    private void checkClientStainless(Version insert, SickServer writer,
            String key) throws Exception {
        Version copyC1 = null;
        Version copyC2 = null;
        Version copyC3 = null;

        long start = -1;
        long delayC1 = -1;
        long delayC2 = -1;
        long delayC3 = -1;
        long timeout = 1000;

        insert(writer, "", key, insert);
        start = timeHandler.getCurrentTime();
        do {
            if (!(copyC1 = read(server1, "", key, null)).isNull() && delayC1 == -1) {
                delayC1 = timeHandler.getCurrentTime() - start;
            }
            if (!(copyC2 = read(server2, "", key, null)).isNull() && delayC2 == -1) {
                delayC2 = timeHandler.getCurrentTime() - start;
            }
            if (!(copyC3 = read(server3, "", key, null)).isNull() && delayC3 == -1) {
                delayC3 = timeHandler.getCurrentTime() - start;
            }

            if (timeHandler instanceof FakeTimeHandler) {
                ((FakeTimeHandler) timeHandler).increaseTime(10);
            }
        } while (timeHandler.getCurrentTime() - start < timeout
                && (delayC1 == -1 || delayC2 == -1 || delayC3 == -1));

        assertEquals(insert, copyC1);
        assertEquals(insert, copyC2);
        assertEquals(insert, copyC3);

        System.out.println("writer: " + writer);
        System.out.println("delay client 1:\t" + delayC1);
        System.out.println("delay client 2:\t" + delayC2);
        System.out.println("delay client 3:\t" + delayC3);
        assertTrue((delayC1 < 50 && server1 == writer) || (450 < delayC1 && delayC1 < 550));
        assertTrue((delayC2 < 50 && server2 == writer) || (450 < delayC2 && delayC2 < 550));
        assertTrue((delayC3 < 50 && server3 == writer) || (450 < delayC3 && delayC3 < 550));

    }

    /**
     * Sends a delete request.
     */
    private boolean delete(SickServer writer, String table, String key)
            throws Exception {
        ClientRequestDelete request = new ClientRequestDelete(table, key);

        ServerResponse response = sendRequest(writer, request);

        return response instanceof ServerResponseDelete;
    }

    /**
     * Sends a insert request.
     */
    private boolean insert(SickServer writer, String table, String key,
            Version insert) throws Exception {
        ClientRequestInsert request = new ClientRequestInsert(table, key,
                insert);

        ServerResponse response = sendRequest(writer, request);

        return response instanceof ServerResponseInsert;
    }

    /**
     * Sends a read request.
     */
    private Version read(SickServer writer, String table, String key,
            Set<String> fields) throws Exception {
        ClientRequestRead request = new ClientRequestRead(table, key, fields);

        ServerResponse response = sendRequest(writer, request);

        return ((ServerResponseRead) response).getVersion();
    }

    /**
     * Sends a scan request.
     *
     * @param writer
     * @param table
     * @param startkey        start the scan at this key
     * @param recordcount     the number of records to read
     * @param fields          read only a specific set of columns
     * @return a list with all found data items.
     */
    private List<Version> scan(SickServer writer, String table,
            String startkey, int recordcount, Set<String> fields)
            throws Exception {
        ClientRequestScan request = new ClientRequestScan(table, startkey,
                recordcount, fields, true);

        ServerResponse response = sendRequest(writer, request);

        if (response instanceof ServerResponseScan) {
            return ((ServerResponseScan) response).getEntries();
        }
        return null;
    }

    /**
     * Send a specific request to the server
     *
     * @param writer
     * @param request
     * @return
     * @throws Exception
     */
    private ServerResponse sendRequest(SickServer writer, ClientRequest request)
        throws Exception {

        request.setReceivedAt(timeHandler.getCurrentTime());
        request.setReceivedBy(writer.getID());

        ServerResponse response = queryHandler.processQuery(writer, request);
        if (response instanceof ServerResponseException) {
            throw ((ServerResponseException) response).getException();
        }

        return response;
    }

    @Test
    public void testStaleness() throws Exception {
        // TODO implement test

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

        checkClientStainless(adele, server1, "adele");
        checkClientStainless(mike, server2, "mike");
        checkClientStainless(john, server3, "john");
        checkClientStainless(bob, server1, "bob");

        List<Version> copies1 = null;
        List<Version> copies2 = null;
        List<Version> copies3 = null;

        // perform range query and compare
        copies2 = scan(server2, "", "adele", 3, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(3, copies2.size());

        // extend range and expect one additional row
        copies2 = scan(server2, "", "adele", 4, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(mike, copies2.get(3));
        assertEquals(4, copies2.size());

        // a larger range should return the same result
        copies2 = scan(server2, "", "adele", 27, null);
        assertEquals(adele, copies2.get(0));
        assertEquals(bob, copies2.get(1));
        assertEquals(john, copies2.get(2));
        assertEquals(mike, copies2.get(3));
        assertEquals(4, copies2.size());

        // have the range begin and end somewhere in the middle
        copies2 = scan(server2, "", "bob", 2, null);
        assertEquals(bob, copies2.get(0));
        assertEquals(john, copies2.get(1));
        assertEquals(2, copies2.size());

        // remove something and do the same scan again
        assertTrue(delete(server2, "", "john"));
        // c2 should see the consequence immediately
        copies1 = scan(server1, "", "bob", 2, null);
        copies2 = scan(server2, "", "bob", 2, null);
        copies3 = scan(server3, "", "bob", 2, null);
        assertEquals(bob, copies2.get(0));
        assertEquals(mike, copies2.get(1));
        assertEquals(2, copies2.size());

        assertEquals(bob, copies1.get(0));
        assertEquals(john, copies1.get(1));
        assertEquals(2, copies1.size());

        assertEquals(bob, copies3.get(0));
        assertEquals(john, copies3.get(1));
        assertEquals(2, copies3.size());

        timeHandler.sleep(600);
        // c2 and c3 should see the consequence immediately not immediately
        // (after about 500 ms)
        copies1 = scan(server1, "", "bob", 2, null);
        copies3 = scan(server3, "", "bob", 2, null);
        assertEquals(bob, copies1.get(0));
        assertEquals(mike, copies1.get(1));
        assertEquals(2, copies1.size());

        assertEquals(bob, copies3.get(0));
        assertEquals(mike, copies3.get(1));
        assertEquals(2, copies3.size());
    }

    @Test
    public void testDelayGenerator() throws Exception{
        queryHandler.setReplicationDelayGenerator(new MongoDbReplicationDelay(100));

        // create some data objects
        Version bob = new Version();
        bob.put("name", "bob");
        bob.put("hair", "brown");
        bob.put("age", 25);

        ClientRequestInsert request = new ClientRequestInsert("", "bob", bob);
        ServerResponse response = sendRequest(server1, request);

        assertEquals((Long) 100l, response.getWaitTimeout());
    }
}
