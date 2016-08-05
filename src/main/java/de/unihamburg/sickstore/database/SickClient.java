package de.unihamburg.sickstore.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;

import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import de.unihamburg.sickstore.database.messages.exception.NotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Wolfram Wingerath
 * 
 */
public class SickClient extends Participant {
    private static final Logger log = LoggerFactory.getLogger("sickstore");
    private int timeout;
    private String host;
    private int tcpPort;
    private String destinationNode;

    /** stores outstanding requests by their identifiers (keys) */
    public final Map<Long, Object> outstandingRequests = new ConcurrentHashMap<>();

    private final SickClient sickclient = this;
    public Client client;
    private TimeHandler timeHandler;

    /**
     * Default constructuro to connect the client to a server.
     *
     * @param timeout
     * @param host
     * @param tcpPort
     * @throws IOException
     */
    @SuppressWarnings("unused")
    public SickClient(int timeout, String host, int tcpPort) throws IOException {
        this(timeout, host, tcpPort, null);
    }

    /**
     * Default constructuro to connect the client to a server.
     *
     * @param timeout
     * @param host
     * @param tcpPort
     * @throws IOException
     */
    @SuppressWarnings("unused")
    public SickClient(int timeout, String host, int tcpPort, String destinationNode) throws IOException {
        this(timeout, host, tcpPort, destinationNode, new SystemTimeHandler());
    }

    /**
     * Default internal constructur, which is only used for testing.
     *
     * @param timeout
     * @param host
     * @param tcpPort
     * @param destinationNode
     * @param timeHandler
     * @throws IOException
     */
    SickClient(int timeout, String host, int tcpPort, String destinationNode, TimeHandler timeHandler)
            throws IOException {
        this.timeout = timeout;
        this.host = host;
        this.tcpPort = tcpPort;
        this.destinationNode = destinationNode;
        this.timeHandler = timeHandler;

        initConnection();
    }

    private void initConnection() throws IOException {
        client = new Client();
        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Participant.register(client);

        new Thread(client).start();

        client.addListener(new ThreadedListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Connected to server.");
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Disconnected from server.");
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof ServerResponse) {
                    // Wait some time if the response wants us to wait
                    ServerResponse response = (ServerResponse) object;
                    long sentByClientAt = response.getSentByClientAt();
                    long now = System.currentTimeMillis();
                    long latency = now - sentByClientAt;
                    long diff = response.getWaitTimeout() - latency;
                    if (diff > 0) {
                        sickclient.timeHandler.sleep(diff);
                    }
                }

                if (object instanceof ServerResponseRead) {
                    ServerResponseRead message = (ServerResponseRead) object;

                    Long id = message.getClientRequestID();
                    Version version = message.getVersion();

                    sickclient.outstandingRequests.put(id, version);
                } else if (object instanceof ServerResponseScan) {
                    ServerResponseScan message = (ServerResponseScan) object;

                    Long id = message.getClientRequestID();
                    List<Version> entries = message.getEntries();

                    sickclient.outstandingRequests.put(id, entries);
                } else if (object instanceof ServerResponseException) {
                    ServerResponseException message = (ServerResponseException) object;

                    Long id = message.getClientRequestID();
                    Exception exception = message.getException();

                    sickclient.outstandingRequests.put(id, exception);
                } else if (object instanceof ServerResponse) {
                    ServerResponse message = (ServerResponse) object;
                    Long id = message.getClientRequestID();
                    sickclient.outstandingRequests.put(id, true);
                } else if (object instanceof FrameworkMessage) {
                } else {
                    System.out.println("connection "
                            + sickclient.client.getID() + " received: "
                            + object);
                }
            }
        }));
    }



    /**
     * Checks whether the client is connected to a server; if not, throws an
     * exception.
     * 
     * @param message
     * 
     * @throws NotConnectedException
     */
    private void checkWhetherConnected(String message)
            throws NotConnectedException {
        if (!client.isConnected()) {
            throw new NotConnectedException(message);
        }
    }

    public void connect() throws IOException {
        client.connect(timeout, host, tcpPort);
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
    public boolean delete(String table, String key, WriteConcern writeConcern) throws DatabaseException {
        checkWhetherConnected("Cannot perform delete operation: not connected to server.");

        ClientRequestDelete request = new ClientRequestDelete(table, key, writeConcern, destinationNode);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
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
     * Fallback, if no write concern was specified.
     *
     * @param table
     * @param key
     * @return
     * @throws DatabaseException
     */
    public boolean delete(String table, String key) throws DatabaseException {
        return delete(table, key, new WriteConcern());
    }

    public void disconnect() {
        client.stop();
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
    public boolean insert(String table, String key, Version values, WriteConcern writeConcern)
            throws DatabaseException {
        checkWhetherConnected("Cannot perform insert operation: not connected to server.");

        ClientRequestInsert request = new ClientRequestInsert(table, key, values, writeConcern, destinationNode);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
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
     * Fallback, if no write concern was specified.
     *
     * @param table
     * @param key
     * @return
     * @throws DatabaseException
     */
    public boolean insert(String table, String key, Version version) throws DatabaseException {
        return insert(table, key, version, new WriteConcern());
    }

    /**
     * 
     * @param table
     *            The name of the table
     * @param key
     *            The record key of the record to read.
     * @param fields
     *            The list of fields to read, or null for all of them
     * @return the read version
     * @throws DatabaseException
     */
    public Version read(String table, String key, Set<String> fields, ReadPreference readPreference)
            throws DatabaseException {
        checkWhetherConnected("Cannot perform read operation: not connected to server.");

        ClientRequestRead request = new ClientRequestRead(table, key, fields, destinationNode, readPreference);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof Version) {
            return (Version) ack;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        } else {
            throw new DatabaseException("Something went wrong.");
        }
    }

    public Version read(String table, String key, Set<String> fields) throws DatabaseException {
        return read(table, key, fields, null);
    }


    /**
     * 
     * @param table
     *            The name of the table
     * @param startkey
     *            The record key of the first record to read.
     * @param recordcount
     *            The number of records to read
     * @param fields
     *            The list of fields to read, or null for all of them
     * @param ascending
     *            indicates whether the keys should be scanned in ascending
     *            (true) or descending order (false)
     * @return the read versions
     * @throws DatabaseException
     */
    public List<Version> scan(String table, String startkey, int recordcount,
            Set<String> fields, boolean ascending, ReadPreference readPreference) throws DatabaseException {
        checkWhetherConnected("Cannot perform scan operation: not connected to server.");

        ClientRequestScan request = new ClientRequestScan(table, startkey, recordcount, fields, ascending, destinationNode,
                readPreference);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
            ack = outstandingRequests.get(request.getId());
        } while (ack == null);

        if (ack instanceof List<?>) {
            List<Version> list = new ArrayList<Version>();
            Object entry = null;
            for (int i = 0; i < ((List<?>) ack).size(); i++) {
                if ((entry = ((List<?>) ack).get(i)) instanceof Version
                        || entry == null) {
                    list.add(i, (Version) entry);
                } else {
                    throw new IllegalArgumentException("Scan result returned: "
                            + entry);
                }
            }
            return list;
        } else if (ack instanceof DatabaseException) {
            throw (DatabaseException) ack;
        } else {
            throw new DatabaseException("Something went wrong.");
        }
    }

    public List<Version> scan(String table, String startkey, int recordcount, Set<String> fields, boolean ascending) throws DatabaseException {
        return scan(table, startkey, recordcount, fields, ascending, null);
    }

    public List<Version> scan(String table, String startkey, int recordcount, Set<String> fields) throws DatabaseException {
        return scan(table, startkey, recordcount, fields, true);
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
    public boolean update(String table, String key, Version values, WriteConcern writeConcern)
            throws DatabaseException {
        checkWhetherConnected("Cannot perform update operation: not connected to server.");

        ClientRequestUpdate request = new ClientRequestUpdate(table, key, values, writeConcern, destinationNode);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
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
     * Fallback, if no write concern was specified.
     *
     * @param table
     * @param key
     * @return
     * @throws DatabaseException
     */
    public boolean update(String table, String key, Version values) throws DatabaseException {
        return update(table, key, values, new WriteConcern());
    }

    /**
     * Request to export measurements and restart measurement
     * @param exportFolder  export measurements to a folder with the given name
     * @return  true on success; false else
     * @throws DatabaseException
     */
    public boolean cleanup(String exportFolder) throws DatabaseException {
        checkWhetherConnected("Cannot perform cleanup operation: not connected to server.");

        checkWhetherConnected("Cannot perform scan operation: not connected to server.");

        ClientRequestCleanup request = new ClientRequestCleanup(exportFolder);
        client.sendTCP(request);

        Object ack;
        do {
            checkWhetherConnected("Connection dropped.");
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
