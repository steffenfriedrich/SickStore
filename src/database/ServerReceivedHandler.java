package database;

import java.util.Set;

import backend.Entry;

import com.esotericsoftware.kryonet.Connection;

import database.messages.ClientRequest;
import database.messages.ClientRequestRead;
import database.messages.ServerResponse;
import database.messages.ServerResponseException;
import database.messages.exception.ExceptionNoColumnProvided;
import database.messages.exception.ExceptionNoKeyProvided;
import database.messages.exception.ExceptionNoValueProvided;
import database.messages.exception.ExceptionUnknownMessageType;

public class ServerReceivedHandler extends Thread {
	private Object object;
	private Connection c;
	private PIMPServer server;

	public ServerReceivedHandler(Connection c, Object object, PIMPServer node) {
		// TODO Auto-generated constructor stub
		this.c = c;
		this.object = object;
		this.server = node;
	}

	@Override
	public void run() {
		Long id = -1l;
		try {
			if (object instanceof ClientRequestRead) {
				ClientRequestRead request = (ClientRequestRead) object;

				id = request.getId();
				String key = request.getKey();
				if (key == null) {
					throw new ExceptionNoKeyProvided(
							"Cannot process get request; no key was provided.");
				}

				Entry entry = server.get(key);
				ServerResponse response = new ServerResponseGet(id, entry);

				server.send(c.getID(), response);

			} else if (object instanceof ClientRequestGetColumn) {
				ClientRequestGetColumn request = (ClientRequestGetColumn) object;

				id = request.getId();
				String key = request.getKey();
				Set<String> column = request.getColumn();

				if (key == null) {
					throw new ExceptionNoKeyProvided(
							"Cannot process get request; no key was provided.");
				} else if (column == null) {
					throw new ExceptionNoColumnProvided(
							"Cannot process put request; no column was provided.");
				}

				Object entry = server.get(key, column);
				ServerResponseGetColumn response = new ServerResponseGetColumn(
						id, entry);

				server.send(c.getID(), response);

			} else if (object instanceof ClientRequestPut) {
				ClientRequestPut request = (ClientRequestPut) object;

				id = request.getId();
				String key = request.getKey();
				Entry value = request.getValue();

				if (key == null) {
					throw new ExceptionNoKeyProvided(
							"Cannot process put request; no key was provided.");
				} else if (value == null) {
					throw new ExceptionNoValueProvided(
							"Cannot process put request; no value was provided.");
				}

				server.put(key, value);
				// acknowledge put request
				ServerResponsePut response = new ServerResponsePut(id);
				server.send(c.getID(), response);

			} else if (object instanceof ClientRequestPutColumn) {
				ClientRequestPutColumn request = (ClientRequestPutColumn) object;

				id = request.getId();
				String key = request.getKey();
				String column = request.getColumn();
				Object value = request.getValue();

				if (key == null) {
					throw new ExceptionNoKeyProvided(
							"Cannot process put request; no key was provided.");
				} else if (column == null) {
					throw new ExceptionNoColumnProvided(
							"Cannot process put request; no column was provided.");
				} else if (value == null) {
					throw new ExceptionNoValueProvided(
							"Cannot process put request; no value was provided.");
				}

				server.put(key, column, value);
				// acknowledge put request
				ServerResponsePut response = new ServerResponsePut(id);
				server.send(c.getID(), response);

			} else if (object instanceof ClientRequest) {
				ClientRequest request = (ClientRequest) object;

				id = request.getId();
				throw new ExceptionUnknownMessageType(
						"Cannot process get request; unknown message type: "
								+ request.getClass());
			} else {
				throw new ExceptionUnknownMessageType(
						"Cannot process get request; unknown message type: "
								+ object.getClass());
			}
		} catch (Exception e) {
			server.send(c.getID(), new ServerResponseException(id, e));
		}
	}
}
