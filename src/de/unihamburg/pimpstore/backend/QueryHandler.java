package de.unihamburg.pimpstore.backend;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihamburg.pimpstore.backend.staleness.ConstantStaleness;
import de.unihamburg.pimpstore.backend.staleness.StalenessGenerator;
import de.unihamburg.pimpstore.database.PIMPServer;
import de.unihamburg.pimpstore.database.messages.ClientRequest;
import de.unihamburg.pimpstore.database.messages.ClientRequestDelete;
import de.unihamburg.pimpstore.database.messages.ClientRequestInsert;
import de.unihamburg.pimpstore.database.messages.ClientRequestRead;
import de.unihamburg.pimpstore.database.messages.ClientRequestScan;
import de.unihamburg.pimpstore.database.messages.ClientRequestUpdate;
import de.unihamburg.pimpstore.database.messages.ServerResponse;
import de.unihamburg.pimpstore.database.messages.ServerResponseDelete;
import de.unihamburg.pimpstore.database.messages.ServerResponseException;
import de.unihamburg.pimpstore.database.messages.ServerResponseInsert;
import de.unihamburg.pimpstore.database.messages.ServerResponseRead;
import de.unihamburg.pimpstore.database.messages.ServerResponseScan;
import de.unihamburg.pimpstore.database.messages.ServerResponseUpdate;
import de.unihamburg.pimpstore.database.messages.exception.DatabaseException;
import de.unihamburg.pimpstore.database.messages.exception.DeleteException;
import de.unihamburg.pimpstore.database.messages.exception.InsertException;
import de.unihamburg.pimpstore.database.messages.exception.NoKeyProvidedException;
import de.unihamburg.pimpstore.database.messages.exception.UnknownMessageTypeException;
import de.unihamburg.pimpstore.database.messages.exception.UpdateException;

public class QueryHandler {
    private static final Logger logStaleness = LoggerFactory
            .getLogger("logStaleness");

    private final static QueryHandler instance;

    static {
        instance = new QueryHandler();
    }

    public static QueryHandler getInstance() {
        return instance;
    }

    /** Generates server IDs, starting with 1 */
    private final AtomicInteger IDGenerator = new AtomicInteger(1);

    private Store mediator = Store.getInstance();

    protected final Set<Integer> servers = new HashSet<Integer>();

    private StalenessGenerator staleness = new ConstantStaleness(0, 0);

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
        mediator.delete(server, key, visibility, timestamp);

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
        version.setWrittenAt(timestamp);
        mediator.insert(server, key, version);

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
        version.setWrittenAt(timestamp);
        mediator.update(server, key, version);

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
                id = ((ClientRequest) request).getId();
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
                throw new UnknownMessageTypeException(
                        "Cannot process request; unknown message type: "
                                + request.getClass());
            }
            

            logStaleness.info("[staleness] ");
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

    public synchronized int deregister(PIMPServer node) {
        if (node == null) {
            throw new NullPointerException("server must not be null!");
        }
        int id = node.getID();
        servers.remove(id);
        return id;
    }

    public synchronized void setStaleness(StalenessGenerator staleness) {
        this.staleness = staleness;
    }
}
