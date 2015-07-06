package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.List;

public interface ShardingStrategy {

    QueryHandlerInterface getTargetShard(ClientRequest request, List<QueryHandlerInterface> shards);
}
