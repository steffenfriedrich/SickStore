package de.unihamburg.sickstore.database;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import de.unihamburg.sickstore.database.messages.ServerResponseException;
import de.unihamburg.sickstore.database.messages.exception.UnknownMessageTypeException;

public class SickServer extends Participant {

    private final QueryHandler queryHandler;
    private final TimeHandler timeHandler;

    private final int port;
    private final int ID;
    private final SickServer node = this;

    protected Server server;

    public SickServer(int port, final QueryHandler queryHandler, final TimeHandler timeHandler) {
        this.queryHandler = queryHandler;
        this.timeHandler = timeHandler;

        this.port = port;

        // register server in database backend
        ID = queryHandler.register(node);
    }

    /**
     *
     * @return the Server's ID
     */
    public int getID() {
        return ID;
    }

    public void send(int connectionID, Object object) {
        server.sendToTCP(connectionID, object);
    }

    /**
     * Start the server process.
     */
    public void start() throws IOException {
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

        queryHandler.deregister(node);
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
                queryHandler.decrementAndGetClientCount();
                System.out.println("Server disconnected from connection "
                        + c.getID());
            }

            @Override
            public void connected(Connection connection) {
                queryHandler.incrementAndGetClientCount();
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
        // Mark the request as received by this server
        request.setReceivedBy(node.getID());
        request.setReceivedAt(timeHandler.getCurrentTime());

        return queryHandler.processQuery(node, request);
    }
}
