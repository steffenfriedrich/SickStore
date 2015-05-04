package de.unihamburg.sickstore.database;

import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;

import de.unihamburg.sickstore.test.SickstoreTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotSame;

import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import de.unihamburg.sickstore.database.messages.exception.DeleteException;
import de.unihamburg.sickstore.database.messages.exception.InsertException;
import de.unihamburg.sickstore.database.messages.exception.UpdateException;

/**
 * @author Wolfram Wingerath
 * 
 */
public class SickClientTest extends SickstoreTestCase{

    private SickClient c1;
    private SickServer server;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // specify connection parameters
        String host = "localhost";
        int timeout = 1200;
        int tcpPort = 54999;

        // Create and start server and clients
        server = new SickServer(tcpPort, queryHandler, timeHandler);
        server.start();

        c1 = new SickClient(timeout, host, tcpPort, "Client 1");
        c1.connect();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        c1.disconnect();
        server.shutdown();
        store.clear();
    }

    @Test
    public void testAPI() throws Exception, DatabaseException {
        // Client 1: write something to the database
        Version bob = new Version();
        bob.put("name", "bob");
        bob.put("hair", "brown");
        bob.put("age", 25);
        c1.insert("", "bob", bob);
        // Client 2: read...
        // ... only Bob's age
        Set<String> columns = new HashSet<String>();
        columns.add("age");
        Version bobCopy = c1.read("", "bob", columns);
        assertEquals(bob.get("age"), bobCopy.get("age"));
        assertNotEquals(bob, bobCopy);
        columns.add("hair");
        bobCopy = c1.read("", "bob", columns);
        assertNotEquals(bob, bobCopy);
        assertEquals(bob.get("hair"), bobCopy.get("hair"));
        bobCopy = c1.read("", "bob", null);
        assertEquals(bob, bobCopy);

        // Create an insert some more objects
        Version john = new Version();
        john.put("name", "john");
        john.put("hair", "yellow");
        john.put("age", 21);

        // test update
        try {
            c1.update("", "john", john);
            fail("Update was expected to fail but succeed");
        } catch (UpdateException e) {
        }

        // test insert
        c1.insert("", "john", john);
        try {
            c1.insert("", "john", john);
            fail("Duplicate insert was expected to fail but succeed");
        } catch (InsertException e) {
        }

        // test delete
        c1.delete("", "john");
        try {
            timeHandler.sleep(5);
            c1.delete("", "john");
            fail("Duplicate delete was expected to fail but succeed");
        } catch (DeleteException e) {
        }

        c1.insert("", "john", john);

        Version adele = new Version();
        adele.put("name", "adele");
        adele.put("hair", "red");
        adele.put("age", 24);
        c1.insert("", "adele", adele);

        Version mike = new Version();
        mike.put("name", "mike");
        mike.put("hair", "blonde");
        mike.put("age", 31);
        c1.insert("", "mike", mike);

        // perform range query and compare
        List<Version> copies = c1.scan("", "adele", 3, null);
        assertEquals(adele, copies.get(0));
        assertEquals(bob, copies.get(1));
        assertEquals(john, copies.get(2));
        assertEquals(3, copies.size());

        // extend range and expect one additional row
        copies = c1.scan("", "adele", 4, null);
        assertEquals(adele, copies.get(0));
        assertEquals(bob, copies.get(1));
        assertEquals(john, copies.get(2));
        assertEquals(mike, copies.get(3));
        assertEquals(4, copies.size());

        // a larger range should return the same result
        copies = c1.scan("", "adele", 27, null);
        assertEquals(adele, copies.get(0));
        assertEquals(bob, copies.get(1));
        assertEquals(john, copies.get(2));
        assertEquals(mike, copies.get(3));
        assertEquals(4, copies.size());

        // perform range query in descending order and compare
        copies = c1.scan("", "mike", 4, null, false);
        assertEquals(mike, copies.get(0));
        assertEquals(john, copies.get(1));
        assertEquals(bob, copies.get(2));
        assertEquals(adele, copies.get(3));
        assertEquals(4, copies.size());

        // have the range begin and end somewhere in the middle
        copies = c1.scan("", "bob", 2, null);
        assertEquals(bob, copies.get(0));
        assertEquals(john, copies.get(1));
        assertEquals(2, copies.size());

        // remove something and do the same scan again
        assertTrue(c1.delete("", "john"));
        copies = c1.scan("", "bob", 2, null);
        assertEquals(bob, copies.get(0));
        assertEquals(mike, copies.get(1));
        assertEquals(2, copies.size());

        // perform range query and compare
        copies = c1.scan("", "adele", 3, null);
        assertEquals(adele, copies.get(0));
        assertEquals(bob, copies.get(1));
        assertEquals(mike, copies.get(2));
        assertEquals(3, copies.size());

        // Not perform an update
        Version mikeOld = new Version();
        mikeOld.put("name", "mike");
        mikeOld.put("hair", "gray");
        mikeOld.put("age", 65);
        c1.update("", "mike", mikeOld);

        // perform range query and compare
        copies = c1.scan("", "adele", 3, null);
        assertEquals(adele, copies.get(0));
        assertEquals(bob, copies.get(1));
        assertEquals(mikeOld, copies.get(2));
        assertNotSame(mikeOld, copies.get(2));
        assertEquals(3, copies.size());

        // now test scan with projection (only part of all columns requested)

        // perform range query and compare
        columns.clear();
        columns.add("name");
        copies = c1.scan("", "adele", 3, columns);
        assertEquals("adele", copies.get(0).get("name"));
        assertEquals(1, copies.get(0).getValues().size());
        assertEquals("bob", copies.get(1).get("name"));
        assertEquals(1, copies.get(1).getValues().size());
        assertEquals("mike", copies.get(2).get("name"));
        assertEquals(1, copies.get(2).getValues().size());
        assertEquals(3, copies.size());
    }

}
