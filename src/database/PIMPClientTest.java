/**
 * 
 */
package database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backend.Entry;

/**
 * @author Wolfram Wingerath
 *
 */
public class PIMPClientTest {

    private PIMPServer server;
    private PIMPClient c1;
    private PIMPClient c2;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        
        //specify  connection parameters
        String host = "localhost";
        int timeout = 120;
        int tcpPort = 54555;

        //Create  and start server and clients
        server = new PIMPServer(tcpPort); 
        c1 = new PIMPClient(timeout, host, tcpPort, "Client 1");
        c1.connect();
        c2 = new PIMPClient(timeout, host, tcpPort, "Client 2");
        c2.connect();  
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {
         String table = "table";
        String key = "1";
        String column = "name";
        String value = "Jonny";
        Entry entry =null;
        Object result = null;
      
        
        
//        
//        
//        //the database is empty 
//        //put-column/get-column
//        //we expect some not-existing value under a not-existing key to be inserted on put
//        c1.insert(table, key, column, value);
//        assertEquals(value, c2.get(key,column));
//        
//        
//        //put/get entire entries
//        entry = new Entry();
//        entry.put("name", "Steffen");
//        entry.put("age", 26);
//        c2.put("2", entry);
//        result =c1.get("2");
//        assertTrue(value!= result);
//        assertEquals(entry, result);
//        
        // TODO put/get: multiple columns 
        
        // TODO Range scans: entire entries
        
        // TODO range scans: single columns
        
        // TODO range scans: multi-column 
        

        //we expect some not-existing value under a not-existing key to be inserted on put
//        c1.put(key, column, value);
//        assertEquals(value, c2.get(key,column));
    }

}
