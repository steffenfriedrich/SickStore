package database.handler;

import com.esotericsoftware.kryonet.Connection;

import database.PIMPClient;

public class ClientConnectedHandler extends Thread {
	private Connection c;
	private PIMPClient pimpclient;

	public ClientConnectedHandler(Connection c, PIMPClient pimpclient) {
		this.c = c;
		this.pimpclient = pimpclient;
	}

	@Override
	public void run() {
		// TODO Auto-generated constructor stub
	}
}
