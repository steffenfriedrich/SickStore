package de.unihamburg.pimpstore.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import de.unihamburg.pimpstore.backend.Version;
import de.unihamburg.pimpstore.database.messages.ClientRequestDelete;
import de.unihamburg.pimpstore.database.messages.ClientRequestInsert;
import de.unihamburg.pimpstore.database.messages.ClientRequestRead;
import de.unihamburg.pimpstore.database.messages.ClientRequestScan;
import de.unihamburg.pimpstore.database.messages.ClientRequestUpdate;
import de.unihamburg.pimpstore.database.messages.ServerResponseDelete;
import de.unihamburg.pimpstore.database.messages.ServerResponseException;
import de.unihamburg.pimpstore.database.messages.ServerResponseInsert;
import de.unihamburg.pimpstore.database.messages.ServerResponseRead;
import de.unihamburg.pimpstore.database.messages.ServerResponseScan;
import de.unihamburg.pimpstore.database.messages.ServerResponseUpdate;
import de.unihamburg.pimpstore.database.messages.exception.DatabaseException;

/**
 * 
 * @author Wolfram Wingerath
 *
 */
public class PIMPClient extends Participant {
    private class ClientReceivedHandler extends Thread {
        private Object object;
        private PIMPClient pimpclient;

        public ClientReceivedHandler(Connection c, Object object,
                PIMPClient pimpclient) {
            this.object = object;
            this.pimpclient = pimpclient;
        }

        @Override
        public void run() {
            if (object instanceof ServerResponseDelete) {
                ServerResponseDelete message = (ServerResponseDelete) object;

                Long id = message.getId();

                pimpclient.outstandingRequests.put(id, true);

            } else if (object instanceof ServerResponseInsert) {
                ServerResponseInsert message = (ServerResponseInsert) object;

                Long id = message.getId();

                pimpclient.outstandingRequests.put(id, true);

            } else if (object instanceof ServerResponseRead) {
                ServerResponseRead message = (ServerResponseRead) object;

                Long id = message.getId();
                Version version = message.getVersion();

                pimpclient.outstandingRequests.put(id, version);

            } else if (object instanceof ServerResponseScan) {
                ServerResponseScan message = (ServerResponseScan) object;

                Long id = message.getId();
                List<Version> entries = message.getEntries();

                pimpclient.outstandingRequests.put(id, entries);

            } else if (object instanceof ServerResponseUpdate) {
                ServerResponseUpdate message = (ServerResponseUpdate) object;

                Long id = message.getId();

                pimpclient.outstandingRequests.put(id, true);

            } else if (object instanceof ServerResponseException) {
                ServerResponseException message = (ServerResponseException) object;

                Long id = message.getId();
                Exception exception = message.getException();

                pimpclient.outstandingRequests.put(id, exception);

            } else {
                System.out.println("connection " + pimpclient.client.getID()
                        + " received: " + object);
            }
        }
    }

    public Client client;
    private String host;
    private String name;
    /** stores outstanding requests by their identifiers (keys) */
    public final Map<Long, Object> outstandingRequests = new ConcurrentHashMap<Long, Object>();

    private final PIMPClient pimpclient = this;

    private int tcpPort;

    private int timeout;

    public PIMPClient() throws IOException {
        client = new Client();
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Participant.register(client);

        client.addListener(new Listener() {
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
                new ClientReceivedHandler(c, object, pimpclient).run();
            }
        });
    }

    public PIMPClient(int timeout, String host, int tcpPort) throws IOException {
        this(timeout, host, tcpPort, null);
    }

    public PIMPClient(int timeout, String host, int tcpPort, String name)
            throws IOException {
        this();

        this.timeout = timeout;
        this.host = host;
        this.tcpPort = tcpPort;
        this.name = name;
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
    public boolean insert(String table, String key, Version values)
            throws DatabaseException {
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
    public Version read(String table, String key, Set<String> fields)
            throws DatabaseException {
        ClientRequestRead request = new ClientRequestRead(table, key, fields);
        client.sendTCP(request);

        Object ack;
        do {
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

    public List<Version> scan(String table, String startkey, int recordcount,
            Set<String> fields) throws DatabaseException {
        return scan(table, startkey, recordcount, fields, true);
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
            Set<String> fields, boolean ascending) throws DatabaseException {
        ClientRequestScan request = new ClientRequestScan(table, startkey,
                recordcount, fields, ascending);
        client.sendTCP(request);

        Object ack;
        do {
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

    @Override
    public String toString() {
        return name;
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
    public boolean update(String table, String key, Version values)
            throws DatabaseException {
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
}
