package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestScan;

import java.util.List;

public interface ShardingStrategy {

    /**
     * Define the target shard that is responsible for the given request (except scan requests).
     *
     * @param request
     * @param shards
     * @return
     */
    QueryHandlerInterface getTargetShard(ClientRequest request, List<QueryHandlerInterface> shards);

    /**
     * Execute the scan request on all shards that need to be asked for the queried range
     * and combine the results.
     *
     * @param request
     * @param shards
     * @return
     */
    List<Version> doScanRequest(ClientRequestScan request, List<QueryHandlerInterface> shards);
}
