package database;

import java.io.IOException;

import backend.QueryHandler;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import database.messages.ClientRequest;

public class PIMPServer extends Participant {
    protected Server server;
    private final PIMPServer node = this;
    private final int port;
    private final int ID;

    public PIMPServer(int port) throws IOException {
        // register server in database backend
        ID = QueryHandler.getInstance().register(node);
        this.port = port;
        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        server.addListener(new Listener() {
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
                            QueryHandler.getInstance().processQuery(node, c,
                                    object);
                        };
                    }.start();
                }
            }

            public void disconnected(Connection c) {
                System.out.println("Server disconnected from connection "
                        + c.getID());
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

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int timeout = 120;
        int tcpPort = 54555;

        Log.set(Log.LEVEL_DEBUG);

        final long t = System.currentTimeMillis();

        // new Thread() {
        // @Override
        // public void run() {
        // while (System.currentTimeMillis() < t + 2000) {
        //
        // }
        //
        // System.exit(-1);
        // }
        // }.start();

        PIMPServer server = new PIMPServer(tcpPort);

        PIMPClient c1 = new PIMPClient(timeout, host, tcpPort, "c1");
        c1.connect();

        PIMPClient c2 = new PIMPClient(timeout, host, tcpPort, "c2");
        c2.connect();
    }
}
