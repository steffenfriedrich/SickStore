package de.unihamburg.sickstore.database;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

public class SickServer extends Participant {
    private final int ID;
    private final SickServer node = this;
    private final int port;

    protected TimeHandler timeHandler;
    protected Server server;

    public SickServer(int port) throws IOException {
        this(port, new SystemTimeHandler());
    }

    public SickServer(int port, final TimeHandler timeHandler) throws IOException {
        // register server in database backend
        ID = QueryHandler.getInstance().register(node);
        this.port = port;
        this.timeHandler = timeHandler;
        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        server.addListener(new Listener() {
            @Override
            public void disconnected(Connection c) {
                QueryHandler.getInstance().decrementAndGetClientCount();
                System.out.println("Server disconnected from connection "
                        + c.getID());
            }

            @Override
            public void connected(Connection connection) {
                QueryHandler.getInstance().incrementAndGetClientCount();
                super.connected(connection);
            }

            @Override
            public void received(final Connection c, final Object object) {
                synchronized (c) {
                    if (!(object instanceof FrameworkMessage)) {
                        if (object instanceof ClientRequest) {
                            // Mark the request as received by this server
                            ((ClientRequest) object).setReceivedBy(node.getID());
                            ((ClientRequest) object).setReceivedAt(
                                    timeHandler.getCurrentTime()
                            );
                        }
                        // new Thread() {
                        // @Override
                        // public void run() {
                        ServerResponse response = QueryHandler.getInstance()
                                .processQuery(node, object);

                        node.send(c.getID(), response);
                        // };
                        // }.start();
                    }
                }
            }
        });
        server.bind(this.port);
        server.start();
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
        QueryHandler.getInstance().deregister(node);
    }

}
