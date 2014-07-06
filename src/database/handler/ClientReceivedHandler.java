package database.handler;

import java.util.List;

import backend.Version;

import com.esotericsoftware.kryonet.Connection;

import database.PIMPClient;
import database.messages.ServerResponseDelete;
import database.messages.ServerResponseException;
import database.messages.ServerResponseInsert;
import database.messages.ServerResponseRead;
import database.messages.ServerResponseScan;
import database.messages.ServerResponseUpdate;

public class ClientReceivedHandler extends Thread {
    private Object object; 
    private PIMPClient pimpclient;

    public ClientReceivedHandler(Connection c, Object object,
            PIMPClient pimpclient) { 
        this.object = object;
        this.pimpclient = pimpclient;
    }

    @Override
    public void run() {
        if (object instanceof ServerResponseDelete) {
            ServerResponseDelete message = (ServerResponseDelete) object;

            Long id = message.getId();

            pimpclient.outstandingRequests.put(id, true);

        } else if (object instanceof ServerResponseInsert) {
            ServerResponseInsert message = (ServerResponseInsert) object;

            Long id = message.getId();

            pimpclient.outstandingRequests.put(id, true);

        } else if (object instanceof ServerResponseRead) {
            ServerResponseRead message = (ServerResponseRead) object;

            Long id = message.getId();
            Version version = message.getVersion();

            pimpclient.outstandingRequests.put(id, version);

        } else if (object instanceof ServerResponseScan) {
            ServerResponseScan message = (ServerResponseScan) object;

            Long id = message.getId();
            List<Version> entries = message.getEntries();

            pimpclient.outstandingRequests.put(id, entries);

        } else if (object instanceof ServerResponseUpdate) {
            ServerResponseUpdate message = (ServerResponseUpdate) object;

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
