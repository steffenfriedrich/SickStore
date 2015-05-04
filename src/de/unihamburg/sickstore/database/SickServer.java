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

public class SickServer extends Participant {

    private final QueryHandler queryHandler;
    private final TimeHandler timeHandler;

    private final int port;
    private final int ID;
    private final SickServer node = this;

    protected Server server;

    public SickServer(int port, final QueryHandler queryHandler, final TimeHandler timeHandler) throws IOException {

        this.queryHandler = queryHandler;
        this.timeHandler = timeHandler;

        // register server in database backend
        ID = queryHandler.register(node);
        this.port = port;

        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        init();
    }

    public int getID() {
        return ID;
    }

    public void send(int connectionID, Object object) {
        server.sendToTCP(connectionID, object);
    }

    /**
     * Stops the server process.
     */
    public void shutdown() {
        server.stop();
        queryHandler.deregister(node);
    }

    /**
     * Register the listeners and start the server.
     *
     * @throws IOException
     */
    private void init() throws IOException  {
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
                        if (object instanceof ClientRequest) {
                            // Mark the request as received by this server
                            ((ClientRequest) object).setReceivedBy(node.getID());
                            ((ClientRequest) object).setReceivedAt(timeHandler.getCurrentTime());
                        }

                        // new Thread() {
                        // @Override
                        // public void run() {
                        ServerResponse response = queryHandler
                                .processQuery(node, object);

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
}
