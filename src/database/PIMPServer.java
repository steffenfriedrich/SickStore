package database;

import java.io.IOException;

import backend.Mediator;
import backend.Store;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

public class PIMPServer extends Participant {
    protected Server server;
    private final PIMPServer node = this;
    private final int port;
    private Store store = Store.getInstance();

    public PIMPServer(int port) throws IOException {
        this.port = port;
        server = new Server();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        super.register(server);

        server.addListener(new Listener() {
            public void received(final Connection c, final Object object) {
                if (!(object instanceof FrameworkMessage)) {
                    new Thread() {
                        public void run() {
                            Mediator.getInstance().processQuery(node, c,object);
                            };
                    }.start();
                }
            }

            public void disconnected(Connection c) {
                        System.out.println("Server disconnected from connection " + c.getID());
            }
        });
        server.bind(port);
        server.start();
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
