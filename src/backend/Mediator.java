/**
 * 
 */
package backend;

import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryonet.Connection;

import database.PIMPServer;
import database.messages.ClientRequest;
import database.messages.ClientRequestDelete;
import database.messages.ClientRequestInsert;
import database.messages.ClientRequestRead;
import database.messages.ClientRequestScan;
import database.messages.ClientRequestUpdate;
import database.messages.ServerResponse;
import database.messages.ServerResponseDelete;
import database.messages.ServerResponseException;
import database.messages.ServerResponseInsert;
import database.messages.ServerResponseRead;
import database.messages.ServerResponseUpdate;
import database.messages.exception.ExceptionNoKeyProvided;
import database.messages.exception.ExceptionUnknownMessageType;

/**
 * 
 * This class is responsible for introducing data-centric staleness by serving
 * stale data to the server nodes. </br> To this end, all servers have to supply
 * the {@link Mediator} instance with a reference to themselves, so that they
 * get their corresponding degree of staleness.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class Mediator {
    private final static Mediator instance;
    private Store store = Store.getInstance();
    static {
        instance = new Mediator();
    }

    private Mediator() {
    }

    public static Mediator getInstance() {
        return instance;
    }

    private Set<PIMPServer> servers = new HashSet<PIMPServer>();

    public Entry get(PIMPServer server, String key) {
        // TODO Auto-generated method stub
        return get(server, key, (Set<String>) null);
    }

    public Entry get(PIMPServer server, String key, Set<String> columns) {
        servers.add(server);
        
        
        // TODO Auto-generated method stub
        return null;
    }

    public Entry get(PIMPServer server, String key, String column) {
        if (column == null) {
            throw new IllegalArgumentException(
                    "The legal argument: column must not be null!");
        }
        Set<String> columns = new HashSet<String>();
        columns.add(column);
        return get(server, key, columns);
    }

    public Entry getRange(PIMPServer server, String key, Set<String> columns) {
        // TODO Auto-generated method stub
        return null;
    }

    public void put(PIMPServer server, String key, Entry version) {
        // TODO Auto-generated method stub
    }

    public void processQuery(PIMPServer server, Connection c, Object request) {
        Long id = -1l;
        ServerResponse response = null;
        try {
            if (request instanceof ClientRequestDelete) {
                response = process(server, (ClientRequestDelete) request);
            } else if (request instanceof ClientRequestInsert) {
                response = process(server, (ClientRequestInsert) request);
            } else if (request instanceof ClientRequestRead) {
                response = process(server, (ClientRequestRead) request);
            } else if (request instanceof ClientRequestScan) {
                response = process(server, (ClientRequestScan) request);
            } else if (request instanceof ClientRequestUpdate) {
                response = process(server, (ClientRequestUpdate) request);
            } else {
                if (request instanceof ClientRequest) {
                    id = ((ClientRequest) request).getId();
                }
                throw new ExceptionUnknownMessageType(
                        "Cannot process request; unknown message type: "
                                + request.getClass());
            }
        } catch (Exception e) {
            server.send(c.getID(), new ServerResponseException(id, e));
        }
        server.send(c.getID(), response);
    }

    private ServerResponseUpdate process(PIMPServer server, ClientRequestUpdate request) {
        // TODO Auto-generated method stub
        return null;
    }

    private ServerResponseInsert process(PIMPServer server, ClientRequestInsert request) {
        // TODO Auto-generated method stub
        return null;
    }

    private ServerResponseDelete process(PIMPServer server, ClientRequestDelete request) {
        // TODO Auto-generated method stub
        return null;
    }

    
    
    
    
    
    
    
    private ServerResponse process(PIMPServer server, ClientRequestScan request) {
        // TODO Auto-generated method stub
    	
        return null;
    }

    private ServerResponseRead process(PIMPServer server, ClientRequestRead request)
            throws ExceptionNoKeyProvided {

        Long id = request.getId();
        String key = request.getKey();
        if (key == null) {
            throw new ExceptionNoKeyProvided(
                    "Cannot process get request; no key was provided.");
        }

        // TODO Auto-generated method stub
        return null;
    } 
}
