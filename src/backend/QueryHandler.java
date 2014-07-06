package backend;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import backend.staleness.ConstantStaleness;
import backend.staleness.StalenessGenerator;
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
import database.messages.ServerResponseScan;
import database.messages.ServerResponseUpdate;
import database.messages.exception.DatabaseException;
import database.messages.exception.DeleteException;
import database.messages.exception.InsertException;
import database.messages.exception.NoKeyProvidedException;
import database.messages.exception.UnknownMessageTypeException;
import database.messages.exception.UpdateException;

public class QueryHandler {

    private final static QueryHandler instance;

    static {
        instance = new QueryHandler();
    }

    public static QueryHandler getInstance() {
        return instance;
    }

    /** Generates server IDs, starting with 1 */
    private final AtomicInteger IDGenerator = new AtomicInteger(1);

    protected final Set<Integer> servers = new HashSet<Integer>();

    private StalenessGenerator staleness = new ConstantStaleness(0, 0);

    private Store mediator = Store.getInstance();

    private QueryHandler() {
    }

    public synchronized Set<Integer> getServers() {
        return servers;
    }

    public synchronized StalenessGenerator getStaleness() {
        return staleness;
    }

    private ServerResponseDelete process(ClientRequestDelete request)
            throws NoKeyProvidedException, DeleteException {
        int server = request.getReceivedBy();
        String key = request.getKey();
        long timestamp = request.getReceivedAt();
        long clientRequestID = request.getId();
        if (key == null) {
            throw new NoKeyProvidedException(
                    "Cannot process delete request; no key was provided.");
        }

        Map<Integer, Long> visibility = staleness.get(server, request);
        Version version = new Version(server, visibility, true);
        mediator.delete(server, key, version, timestamp);

        ServerResponseDelete response = new ServerResponseDelete(
                clientRequestID);
        return response;
    }

    private ServerResponseInsert process(ClientRequestInsert request)
            throws NoKeyProvidedException, InsertException {
        int server = request.getReceivedBy();
        String key = request.getKey();
        long timestamp = request.getReceivedAt();
        long clientRequestID = request.getId();
        Version version = request.getVersion();
        if (key == null) {
            throw new NoKeyProvidedException(
                    "Cannot process get request; no key was provided.");
        }

        Map<Integer, Long> visibility = staleness.get(server, request);
        version.setVisibility(visibility);
        mediator.insert(server, key, version, timestamp);

        ServerResponseInsert response = new ServerResponseInsert(
                clientRequestID);
        return response;
    }

    private ServerResponseRead process(ClientRequestRead request)
            throws NoKeyProvidedException {

        int server = request.getReceivedBy();
        String key = request.getKey();
        Set<String> columns = request.getFields();
        long timestamp = request.getReceivedAt();
        long clientRequestID = request.getId();
        if (key == null) {
            throw new NoKeyProvidedException(
                    "Cannot process get request; no key was provided.");
        }

        Version version = mediator.get(server, key, columns, timestamp);
        if (version == null) {
            throw new NullPointerException("Version must not be null!");
        }
        ServerResponseRead response = new ServerResponseRead(clientRequestID,
                version);
        return response;
    }

    private ServerResponseScan process(ClientRequestScan request)
            throws NoKeyProvidedException {
        int server = request.getReceivedBy();
        String key = request.getKey();
        int range = request.getRecordcount();
        boolean asc = request.isAscending();
        Set<String> columns = request.getFields();
        long timestamp = request.getReceivedAt();
        long clientRequestID = request.getId();
        if (key == null) {
            throw new NoKeyProvidedException(
                    "Cannot process get request; no key was provided.");
        }

        List<Version> versions = mediator.getRange(server, key, range, asc,
                columns, timestamp);
        ServerResponseScan response = new ServerResponseScan(clientRequestID,
                versions);
        return response;
    }

    private ServerResponseUpdate process(ClientRequestUpdate request)
            throws NoKeyProvidedException, UpdateException {
        int server = request.getReceivedBy();
        String key = request.getKey();
        long timestamp = request.getReceivedAt();
        long clientRequestID = request.getId();
        Version version = request.getVersion();
        if (key == null) {
            throw new NoKeyProvidedException(
                    "Cannot process get request; no key was provided.");
        }

        Map<Integer, Long> visibility = staleness.get(server, request);
        version.setVisibility(visibility);
        mediator.update(server, key, version, timestamp);

        ServerResponseUpdate response = new ServerResponseUpdate(
                clientRequestID);
        return response;
    }

    public synchronized ServerResponse processQuery(PIMPServer server,
            Object request) {
        Long id = -1l;
        ServerResponse response = null;
        try {
            if (request instanceof ClientRequest) {
                int activeServer = ((ClientRequest) request).getReceivedBy();
                if (activeServer != server.getID()) {
                    throw new DatabaseException(
                            "Inconsistent: Request is not handled by the server that received it...  This should not be possible...");
                }
            }

            if (request instanceof ClientRequestDelete) {
                response = process((ClientRequestDelete) request);
            } else if (request instanceof ClientRequestInsert) {
                response = process((ClientRequestInsert) request);
            } else if (request instanceof ClientRequestRead) {
                response = process((ClientRequestRead) request);
            } else if (request instanceof ClientRequestScan) {
                response = process((ClientRequestScan) request);
            } else if (request instanceof ClientRequestUpdate) {
                response = process((ClientRequestUpdate) request);
            } else {
                if (request instanceof ClientRequest) {
                    id = ((ClientRequest) request).getId();
                }
                throw new UnknownMessageTypeException(
                        "Cannot process request; unknown message type: "
                                + request.getClass());
            }
        } catch (Exception e) {
            response = new ServerResponseException(id, e);
            e.printStackTrace();
        }
        return response;
    }

    public synchronized int register(PIMPServer node) {
        int newServerID = IDGenerator.getAndIncrement();
        servers.add(newServerID);
        return newServerID;
    }

    public synchronized void setStaleness(StalenessGenerator staleness) {
        this.staleness = staleness;
    }
}
