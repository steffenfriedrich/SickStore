package database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import backend.Entry;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import database.handler.ClientConnectedHandler;
import database.handler.ClientDisconnectedHandler;
import database.handler.ClientReceivedHandler;
import database.messages.ClientRequestDelete;
import database.messages.ClientRequestInsert;
import database.messages.ClientRequestRead;
import database.messages.ClientRequestScan;
import database.messages.ClientRequestUpdate;
import database.messages.exception.DatabaseException;

public class PIMPClient extends Participant {
    public Client client;
    private String name;
    private final PIMPClient pimpclient = this;
    private int timeout;
    private String host;
    private int tcpPort;

    /** stores outstanding requests by their identifiers (keys) */
    public final Map<Long, Object> outstandingRequests = new ConcurrentHashMap<Long, Object>();

    public PIMPClient() {
        client = new Client();
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Participant.register(client);

        client.addListener(new Listener() {
            public void received(Connection c, Object object) {
                new ClientReceivedHandler(c, object, pimpclient).run();
            }

            public void disconnected(Connection c) {
                new ClientDisconnectedHandler(c, pimpclient).run();
            }

            public void connected(Connection connection) {
                new ClientConnectedHandler(connection, pimpclient).run();
            }
        });
    }

    public PIMPClient(int timeout, String host, int tcpPort, String name)
            throws IOException {
        this();

        this.timeout = timeout;
        this.host = host;
        this.tcpPort = tcpPort;
        this.name = name;
    }

    public PIMPClient(int timeout, String host, int tcpPort) throws IOException {
        this(timeout, host, tcpPort, null);
    }

    public void connect() throws IOException {
        client.connect(timeout, host, tcpPort);
    }

    public void disconnect() {
        client.stop();
    }

  

    public static void main(String[] args) {
        Log.set(Log.LEVEL_DEBUG);
        new PIMPClient();
    }

    /**
     * Read a record from the database. Each field/value pair from the result
     * will be stored in a HashMap.
     * 
     * @param table
     *            The name of the table
     * @param key
     *            The record key of the record to read.
     * @param fields
     *            The list of fields to read, or null for all of them
     * @param result
     *            A HashMap of field/value pairs for the result
     * @return Zero on success, a non-zero error code on error or "not found".
     * @throws DatabaseException 
     */
    public Entry read(String table, String key, Set<String> fields) throws DatabaseException {
        ClientRequestRead request = new ClientRequestRead(table, key, fields);
        client.sendTCP(request);

        Object ack;
        do {
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof Entry) {
            return (Entry) ack;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        }else {
            throw new DatabaseException("Something went wrong.");
        }
    }

    /**
     * Perform a range scan for a set of records in the database. Each
     * field/value pair from the result will be stored in a HashMap.
     * 
     * @param table
     *            The name of the table
     * @param startkey
     *            The record key of the first record to read.
     * @param recordcount
     *            The number of records to read
     * @param fields
     *            The list of fields to read, or null for all of them
     * @param result
     *            A Vector of HashMaps, where each HashMap is a set field/value
     *            pairs for one record
     * @return Zero on success, a non-zero error code on error. See this class's
     *         description for a discussion of error codes.
     * @throws DatabaseException 
     */
    public List<Entry> scan(String table, String startkey, int recordcount,
            Set<String> fields) throws DatabaseException {
        ClientRequestScan request = new ClientRequestScan(table, startkey,
                recordcount, fields);
        client.sendTCP(request);

        Object ack;
        do {
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof List<?>) {
            List<Entry> list = new ArrayList<Entry>();
            Object entry = null;
            for (int i = 0; i < ((List<?>) ack).size(); i++) {
                if ((entry = ((List<?>) ack).get(i)) instanceof Entry
                        || entry == null) {
                    list.add(i, (Entry) entry);
                } else {
                    throw new IllegalArgumentException("Scan result returned: "
                            + entry);
                }
            }
            return list;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        }else {
            throw new DatabaseException("Something went wrong.");
        }
    }

    /**
     * Update a record under the given key
     * 
     * @param table
     *            The name of the table
     * @param key
     *            The record key of the record to write.
     * @param values
     *            field/value pairs to update under the given key
     * @return true on success; false else
     * @throws DatabaseException 
     */
    public boolean update(String table, String key, Entry values) throws DatabaseException {
        ClientRequestUpdate request = new ClientRequestUpdate(table, key,
                values);
        client.sendTCP(request);

        Object ack;
        do {
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof Boolean) {
            return (Boolean) ack;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        }
        return false;
    }

    /**
     * Inserts field/value pairs into the database
     * 
     * @param table
     *            The name of the table
     * @param key
     *            The record key of the record to insert.
     * @param values
     *            field/value pairs to insert under the given key
     * @return true on success; false else
     * @throws DatabaseException 
     */
    public boolean insert(String table, String key, Entry values) throws DatabaseException {
        ClientRequestInsert request = new ClientRequestInsert(table, key,
                values);
        client.sendTCP(request);

        Object ack;
        do {
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof Boolean) {
            return (Boolean) ack;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        }
        return false;
    }

    /**
     * Delete a record from the database.
     * 
     * @param table
     *            The name of the table
     * @param key
     *            The record key of the record to delete.
     * @return true on success; false else
     * @throws DatabaseException 
     */
    public boolean delete(String table, String key) throws DatabaseException {
        ClientRequestDelete request = new ClientRequestDelete(table, key);
        client.sendTCP(request);

        Object ack;
        do {
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof Boolean) {
            return (Boolean) ack;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        }
        return false;
    }
}
