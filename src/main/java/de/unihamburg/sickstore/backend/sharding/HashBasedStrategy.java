package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.util.List;
import java.util.zip.CRC32;

public class HashBasedStrategy implements ShardingStrategy {

    @Override
    public QueryHandlerInterface getTargetShard(ClientRequest request,
                                                List<QueryHandlerInterface> shards) {
        CRC32 crc = new CRC32();
        crc.update(request.getKey().getBytes());

        long hash = crc.getValue();
        long shardIndex = hash % shards.size();

        return shards.get((int) shardIndex);
    }
}
