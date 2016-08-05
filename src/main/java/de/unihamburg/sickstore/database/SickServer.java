package de.unihamburg.sickstore.database;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import com.esotericsoftware.minlog.Log;
import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import de.unihamburg.sickstore.database.messages.ServerResponseException;
import de.unihamburg.sickstore.database.messages.exception.UnknownMessageTypeException;

public class SickServer extends Participant {

    private final QueryHandlerInterface queryHandler;

    private final int port;
    private final SickServer node = this;

    protected Server server;

    /** the overall number of clients connected to the entirety of all SickStore nodes */
    private final AtomicInteger clientCount = new AtomicInteger(0);

    /**
     * Creates a new instance from a given config object.
     *
     * @param config
     */
    @SuppressWarnings("unused")
    public static SickServer newInstanceFromConfig(Map<String, Object> config) {
        int port = (int) config.getOrDefault("port", 54000);
        QueryHandlerInterface queryHandler = (QueryHandlerInterface) InstanceFactory.newInstanceFromConfig(
            (Map<String, Object>) config.get("queryHandler")
        );

        return new SickServer(port, queryHandler);
    }

    public SickServer(int port, final QueryHandlerInterface queryHandler) {
        this.port = port;
        this.queryHandler = queryHandler;
    }

    public void send(int connectionID, Object object) {
        server.sendToTCP(connectionID, object);
    }

    /**
     * Start the server process.
     */
    public void start() throws IOException {
        Log.set(Log.LEVEL_DEBUG);
        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        initServer();
    }

    /**
     * Stops the server process.
     */
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Register the listeners and start the server.
     *
     * @throws IOException
     */
    private void initServer() throws IOException  {
        server.addListener(new Listener() {
            @Override
            public void disconnected(Connection c) {
                decrementAndGetClientCount();
                System.out.println("Server disconnected from connection "
                        + c.getID());
            }

            @Override
            public void connected(Connection connection) {
                incrementAndGetClientCount();
                super.connected(connection);
            }

            @Override
            public void received(final Connection connection, final Object object) {
                synchronized (connection) {
                    if (!(object instanceof FrameworkMessage)) {
                        // new Thread() {
                        // @Override
                        // public void run() {

                        ServerResponse response = null;

                        if (object instanceof ClientRequest) {
                            response = handleRequest((ClientRequest) object);
                        } else {
                            response = new ServerResponseException(
                                -1,
                                new UnknownMessageTypeException(
                                    "Cannot process request; unknown message type: " + object.getClass()
                                )
                            );
                        }

                        node.send(connection.getID(), response);
                        // };
                        // }.start();
                    }
                }
            }
        });

        server.bind(this.port);
        server.start();
    }

    protected ServerResponse handleRequest(ClientRequest request) {
        return queryHandler.processQuery(request);
    }

    /**
     * Increment the number of connection clients and return the current number.
     *
     * @return number of connected clients
     */
    public int incrementAndGetClientCount() {
        clientCount.incrementAndGet();
        return clientCount.get();
    }

    /**
     * Decrement the number of connection clients and return the current number.
     *
     * @return number of connected clients
     */
    public int decrementAndGetClientCount() {
        clientCount.decrementAndGet();
        return clientCount.get();
    }
}
