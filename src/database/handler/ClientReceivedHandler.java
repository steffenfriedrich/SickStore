package database.handler;

import backend.Entry;

import com.esotericsoftware.kryonet.Connection;

import database.PIMPClient;
import database.messages.ServerResponseException;
import database.messages.ServerResponseGet;
import database.messages.ServerResponseGetColumn;
import database.messages.ServerResponsePut;

public class ClientReceivedHandler extends Thread {
    private Object object;
    private Connection c;
    private PIMPClient pimpclient;

    public ClientReceivedHandler(Connection c, Object object,
            PIMPClient pimpclient) {
        this.c = c;
        this.object = object;
        this.pimpclient = pimpclient;
    }

    @Override
    public void run() {
        if (object instanceof ServerResponseGet) {
            ServerResponseGet message = (ServerResponseGet) object;

            Long id = message.getId();
            Entry entry = message.getEntry();

            pimpclient.outstandingRequests.put(id, entry);

        } else if (object instanceof ServerResponseGetColumn) {
            ServerResponseGetColumn message = (ServerResponseGetColumn) object;

            Long id = message.getId(); 
            Object entry = message.getValue();

            pimpclient.outstandingRequests.put(id, entry);

        } else if (object instanceof ServerResponsePut) {
            ServerResponsePut message = (ServerResponsePut) object;

            Long id = message.getId();

            pimpclient.outstandingRequests.put(id, true);

        } else if (object instanceof ServerResponseException) {
            ServerResponseException message = (ServerResponseException) object;

            Long id = message.getId();
            Exception exception = message.getException();

            pimpclient.outstandingRequests.put(id, exception);

        } else {
            System.out.println("connection " + pimpclient.client.getID()
                    + " received: " + object);
        }
    }
}
