package database;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import backend.Entry;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import database.handler.ClientConnectedHandler;
import database.handler.ClientDisconnectedHandler;
import database.handler.ClientReceivedHandler;
import database.messages.ClientRequestGet;
import database.messages.ClientRequestGetColumn;
import database.messages.ClientRequestPut;
import database.messages.ClientRequestPutColumn;
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

    public PIMPClient(int timeout, String host, int tcpPort)
            throws IOException {
        this(timeout, host,tcpPort,  null);
    }

    public void connect() throws IOException {
        client.connect(timeout, host, tcpPort);
    }

    public void disconnect() {
        client.stop();
    }

    public Entry get(String key) throws Exception {
        ClientRequestGet request = new ClientRequestGet(key);
        client.sendTCP(request);

        Object entry;
        do {
            entry = outstandingRequests.get(request.getId());
        } while (entry == null);

        if (entry instanceof Entry) {
            return (Entry) entry;
        } else if (entry instanceof DatabaseException) {
            throw (DatabaseException) entry;
        } else {
            throw new InvalidClassException(
                    "The get request returned object of invalid class: "
                            + entry.getClass().getName());
        }
    }

    public Object get(String key, String column) throws Exception {
        ClientRequestGetColumn request = new ClientRequestGetColumn(key, column);
        client.sendTCP(request);

        Object value;
        do {
            value = outstandingRequests.get(request.getId());
        } while (value == null);

        if (value instanceof DatabaseException) {
            throw (DatabaseException) value;
        } else {
return value;
        }
    }

    public boolean put(String key, Entry value) throws DatabaseException {
        ClientRequestPut request = new ClientRequestPut(key, value);
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

    public boolean put(String key, String column, Object value)
            throws DatabaseException {
        ClientRequestPutColumn request = new ClientRequestPutColumn(key,
                column, value);
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

    public static void main(String[] args) {
        Log.set(Log.LEVEL_DEBUG);
        new PIMPClient();
    }

}
