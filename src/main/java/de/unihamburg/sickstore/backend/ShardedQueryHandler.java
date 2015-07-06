package de.unihamburg.sickstore.backend;

import de.unihamburg.sickstore.backend.sharding.ShardingStrategy;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;

import java.util.*;

/**
 * The sharded query handler redirects all queries to a specific shard (which is a QueryHandlerInterface).
 */
public class ShardedQueryHandler implements QueryHandlerInterface {

    /** contains all shards */
    private List<QueryHandlerInterface> shards = new ArrayList<>();

    private ShardingStrategy strategy;

    public ShardedQueryHandler(List<QueryHandlerInterface> shards, ShardingStrategy strategy) {
        this.shards = shards;
        this.strategy = strategy;
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
            // Currently the range is resolved and merged from all shards
            // and therefore, some shards cannot answer the request properly.
            // Furthermore, the results cannot be merged correctly, because a Version
            // does not save its key.

            return new ServerResponseException(request.getId(), new Exception("Scanning is not " +
                "supported with the shared SickStore at the moment."));

//            List<Version> versions = new ArrayList<>();
//            for (QueryHandlerInterface shard : shards) {
//                ServerResponseScan response = (ServerResponseScan) shard.processQuery(request);
//                versions.addAll(response.getEntries());
//            }
//
//            return new ServerResponseScan(request.getId(), versions);
        }

        // this should not happen, all request types were handled above
        return null;
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
        return strategy.getTargetShard(request, shards);
    }
}
