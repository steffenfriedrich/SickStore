package database;

import java.io.IOException;

import backend.QueryHandler;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import database.messages.ClientRequest;
import database.messages.ServerResponse;

public class PIMPServer extends Participant {
    private final int ID;
    private final PIMPServer node = this;
    private final int port;
    protected Server server;

    public PIMPServer(int port) throws IOException {
        // register server in database backend
        ID = QueryHandler.getInstance().register(node);
        this.port = port;
        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        server.addListener(new Listener() {
            public void disconnected(Connection c) {
                System.out.println("Server disconnected from connection "
                        + c.getID());
            }

            public void received(final Connection c, final Object object) {
                if (!(object instanceof FrameworkMessage)) {
                    if (object instanceof ClientRequest) {
                        // Mark the request as received by this server
                        ((ClientRequest) object).setReceivedBy(node.getID());
                        ((ClientRequest) object).setReceivedAt(System
                                .currentTimeMillis());
                    }
                    new Thread() {
                        public void run() {
                            ServerResponse response = QueryHandler
                                    .getInstance().processQuery(node, object);

                            node.send(c.getID(), response);
                        };
                    }.start();
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
    }

}
