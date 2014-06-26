package database.handler;

import com.esotericsoftware.kryonet.Connection;

import database.PIMPClient;

public class ClientDisconnectedHandler extends Thread {
    private Object object;
    private Connection c;
    private PIMPClient pimpclient;

    public ClientDisconnectedHandler(Connection c, PIMPClient pimpclient) {
        this.c = c;
        this.pimpclient = pimpclient;
    }

    @Override
    public void run() {
        // TODO Auto-generated constructor stub
    }
}
