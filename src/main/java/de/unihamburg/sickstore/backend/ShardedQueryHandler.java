package de.unihamburg.sickstore.backend;

import de.unihamburg.sickstore.backend.sharding.ShardingStrategy;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;

import java.util.*;

/**
 * The sharded query handler redirects all queries to a specific shard (which is a QueryHandlerInterface).
 * The sharding is organized by tables.
 */
public class ShardedQueryHandler implements QueryHandlerInterface {

    /** contains all shards */
    private List<QueryHandlerInterface> shards = new ArrayList<>();

    /** Maps a table name to a sharding strategy */
    private Map<String, ShardingStrategy> strategies;

    public ShardedQueryHandler(List<QueryHandlerInterface> shards, Map<String, ShardingStrategy> strategies) {
        this.shards = shards;
        this.strategies = strategies;
    }

    @Override
    public ServerResponse processQuery(ClientRequest request) {
        // if a specific destination node is given, respect that
        if (request.getDestinationNode() != null) {
            try {
                return getShardByNodeName(request.getDestinationNode()).processQuery(request);
            } catch(DatabaseException e) {
                e.printStackTrace();
                return new ServerResponseException(-1, e);
            }
        }

        // otherwise redirect request to appropriate shard
        if (request instanceof ClientWriteRequest ||
            request instanceof ClientRequestRead) {
            return getTargetShard(request).processQuery(request);
        }

        if (request instanceof ClientRequestScan) {
            return processScanRequest((ClientRequestScan) request);
        }

        // this should not happen, all request types were handled above
        throw new RuntimeException("Unknown request type: " + request.getClass().getName());
    }

    /**
     * Processes a scan request. It is send to all shards and combined afterwards.
     *
     * @param request
     * @return
     */
    private ServerResponse processScanRequest(ClientRequestScan request) {
        // at first collect all versions, to sort them
        TreeMap<String, Version> orderedVersions = new TreeMap<>();
        for (QueryHandlerInterface shard : shards) {
            ServerResponseScan response = (ServerResponseScan) shard.processQuery(request);

            for (Version version : response.getEntries()) {
                orderedVersions.put(version.getKey(), version);
            }
        }

        // afterwards select only the required number of versions
        int range = request.getRecordcount();
        boolean asc = request.isAscending();

        List<Version> versions = new ArrayList<>(range);
        String nextKey = request.getKey();

        do {
            versions.add(orderedVersions.get(nextKey));
        } while (range > versions.size() && (
            (asc && (nextKey = orderedVersions.higherKey(nextKey)) != null) ||
                (!asc && (nextKey = orderedVersions.lowerKey(nextKey)) != null)
        ));

        return new ServerResponseScan(request.getId(), versions);
    }

    @Override
    public void resetMeters() {
        for (QueryHandlerInterface shard : shards) {
            shard.resetMeters();
        }
    }

    private QueryHandlerInterface getShardByNodeName(String name) throws DatabaseException {
        for (QueryHandlerInterface shard : shards) {
            if (!(shard instanceof QueryHandler)) {
                continue;
            }

            for (Node node : ((QueryHandler) shard).getNodes()) {
                if (node.getName().equals(name)) {
                    return shard;
                }
            }
        }

        throw new DatabaseException("Did not find node with name " + name);
    }

    private QueryHandlerInterface getTargetShard(ClientRequest request) {
        ShardingStrategy strategy = strategies.get(request.getTable());
        if (strategy == null) {
            // if no strategy is defined use the first shard
            return shards.iterator().next();
        }

        return strategy.getTargetShard(request, shards);
    }
}
