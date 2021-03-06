package de.unihamburg.sickstore.database.client;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.ReadPreference;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


abstract public class SickClient implements Client {
    private static final Logger logger = LoggerFactory.getLogger(SickStoreConnectionPool.class);

    private final String host;
    private final int port;
    private final String destinationNode;
    private final TimeHandler timeHandler;

    SickConnection.ConnectionFactory connectionFactory;

    public SickClient(String host, int port, String destinationNode, TimeHandler timeHandler) {
        this.host = host;
        this.port = port;
        this.destinationNode = destinationNode;
        this.timeHandler = timeHandler;
        this.connectionFactory = new SickConnection.ConnectionFactory(this);
    }

    abstract public Connection getConnection() throws SQLException;

    abstract public ServerResponse send(ClientRequest request) throws SQLException;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDestinationNode() {
        return destinationNode;
    }

    public TimeHandler getTimeHandler() {
        return timeHandler;
    }

    public SickConnection.ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    abstract public void disconnect();

    /**
     * Inserts field/value pairs into the database
     *
     * @param table        The name of the table
     * @param key          The record key of the record to insert.
     * @param values       field/value pairs to insert under the given key
     * @param table
     * @param key
     * @param values
     * @param writeConcern
     * @return
     * @throws DatabaseException
     * @throws ExecutionException
     */
    public boolean insert(String table, String key, Version values, WriteConcern writeConcern)
            throws Exception {
        ClientRequestInsert request = new ClientRequestInsert(table, key, values, writeConcern, destinationNode);
        Object ack = send(request);

        if (ack instanceof ServerResponseInsert) {
            waitForServerHickup((ServerResponseInsert) ack);
            return true;
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for insert operation");
        }
    }

    public boolean insert(String table, String key, Version version) throws Exception {
        return insert(table, key, version, new WriteConcern());
    }

    /**
     * @param table  The name of the table
     * @param key    The record key of the record to read.
     * @param fields The list of fields to read, or null for all of them
     * @return the read version
     * @throws DatabaseException
     */
    public Version read(String table, String key, Set<String> fields, ReadPreference readPreference)
            throws Exception {
        ClientRequestRead request = new ClientRequestRead(table, key, fields, destinationNode, readPreference);
        Object ack = send(request);
        if (ack instanceof ServerResponseRead) {
            ServerResponseRead response = (ServerResponseRead) ack;
            waitForServerHickup(response);

            return response.getVersion();
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for read operation");
        }
    }

    public Version read(String table, String key, Set<String> fields) throws Exception {
        return read(table, key, fields, null);
    }


    /**
     * @param table       The name of the table
     * @param startkey    The record key of the first record to read.
     * @param recordcount The number of records to read
     * @param fields      The list of fields to read, or null for all of them
     * @param ascending   indicates whether the keys should be scanned in ascending
     *                    (true) or descending order (false)
     * @return the read versions
     * @throws DatabaseException
     */
    public List<Version> scan(String table, String startkey, int recordcount,
                              Set<String> fields, boolean ascending, ReadPreference readPreference)
            throws Exception {
        ClientRequestScan request = new ClientRequestScan(table, startkey, recordcount, fields, ascending, destinationNode,
                readPreference);
        Object ack = send(request);

        if (ack instanceof ServerResponseScan) {
            ServerResponseScan response = (ServerResponseScan) ack;
            waitForServerHickup(response);
            return response.getEntries();
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for scan operation");
        }
    }

    public List<Version> scan(String table, String startkey, int recordcount, Set<String> fields, boolean ascending)
            throws Exception {
        return scan(table, startkey, recordcount, fields, ascending, null);
    }

    public List<Version> scan(String table, String startkey, int recordcount, Set<String> fields)
            throws Exception {
        return scan(table, startkey, recordcount, fields, true);
    }

    /**
     * Update a record under the given key
     *
     * @param table  The name of the table
     * @param key    The record key of the record to write.
     * @param values field/value pairs to update under the given key
     * @return true on success; false else
     * @throws DatabaseException
     */
    public boolean update(String table, String key, Version values, WriteConcern writeConcern)
            throws Exception {
        ClientRequestUpdate request = new ClientRequestUpdate(table, key, values, writeConcern, destinationNode);
        Object ack = send(request);

        if (ack instanceof ServerResponseUpdate) {
            waitForServerHickup((ServerResponseUpdate) ack);
            return true;
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for update operation");
        }
    }


    /**
     * Fallback, if no write concern was specified.
     *
     * @param table
     * @param key
     * @return
     * @throws DatabaseException
     */
    public boolean update(String table, String key, Version values) throws Exception {
        return update(table, key, values, new WriteConcern());
    }

    public boolean delete(String table, String key, WriteConcern writeConcern) throws Exception {
        ClientRequestDelete request = new ClientRequestDelete(table, key, writeConcern, destinationNode);
        Object ack = send(request);

        if (ack instanceof ServerResponseDelete) {
            waitForServerHickup((ServerResponseDelete) ack);
            return true;
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for delete operation");
        }
    }

    /**
     * Fallback, if no write concern was specified.
     *
     * @param table
     * @param key
     * @return
     * @throws DatabaseException
     */
    public boolean delete(String table, String key) throws Exception {
        return delete(table, key, new WriteConcern());
    }

    /**
     * Request to export measurements and restart measurement
     *
     * @param exportFolder export measurements to a folder with the given name
     * @return true on success; false else
     * @throws DatabaseException
     */
    public boolean cleanup(String exportFolder) throws Exception {
        ClientRequestCleanup request = new ClientRequestCleanup(exportFolder);
        Object ack = send(request);

        if (ack instanceof ServerResponseCleanup) {
            waitForServerHickup((ServerResponseCleanup) ack);
            return true;
        } else if (ack instanceof ServerResponseException) {
            throw ((ServerResponseException) ack).getException();
        } else {
            throw new DatabaseException("received wrong response of type:" + ack + " for cleanup operation");
        }
    }


    public void waitForServerHickup(ServerResponse response) {
        long sentByClientAt = response.getSentByClientAt();
        long now = System.currentTimeMillis();
        long latency = now - sentByClientAt;
        long diff = response.getWaitTimeout() - latency;
        if (diff > 0) {
            try {
                Thread.sleep(diff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
