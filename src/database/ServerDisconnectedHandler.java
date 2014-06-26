package database;

import com.esotericsoftware.kryonet.Connection;

public class ServerDisconnectedHandler extends Thread {
    private Object object;
    private Connection c;
    private PIMPServer node;

    public ServerDisconnectedHandler(Connection c, PIMPServer node) {
        // TODO Auto-generated constructor stub
        this.c = c;
        this.node = node;
    }

    @Override
    public void run() {
        System.out.println("Server disconnected from connection " + c.getID());
    }
}
