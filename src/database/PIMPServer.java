package database;

import java.io.IOException;

import backend.Entry;
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
            public void received(Connection c, Object object) {
                if (!(object instanceof FrameworkMessage)) {
                    new ServerReceivedHandler(c, object, node).start();
                }
            }

            public void disconnected(Connection c) {
                new ServerDisconnectedHandler(c, node).start();
            }
        });
        server.bind(port);
        server.start();
    }

    protected void send(int connectionID, Object object) {
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

        Entry entry = new Entry();
        entry.put("name", "Jonny");
        server.put("1", entry);

        PIMPClient c1 = new PIMPClient(timeout, host, tcpPort, "c1");
        c1.connect();

        Entry value = c1.get("1");
        value.put("age", 26);

        System.out.println(c1.put("2", value));
    }

    public Entry get(String key) {
        return store.get(key);
    }

    public void put(String key, Entry value) {
        store.put(key, value);
    }

    /**
     * Updates the given column under the given key with the given value. If
     * there is no entry under the given key, a new entry is created. If the
     * given column is <code>null</code>, it is removed.
     * 
     * @param key
     * @param column
     * @param value
     */
    public synchronized void put(String key, String column, Object value) {
        store.put(key, column, value);
    }

    public synchronized Object get(String key, String column) {
        return store.get(key, column);
    }
}
