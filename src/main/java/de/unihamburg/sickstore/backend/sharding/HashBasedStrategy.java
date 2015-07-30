package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestScan;
import de.unihamburg.sickstore.database.messages.ServerResponseScan;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
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

    /**
     * Do a scan request. With hash-based sharding, each shard needs to be scanned.
     *
     * @param request
     * @param shards
     * @return
     */
    @Override
    public List<Version> doScanRequest(ClientRequestScan request, List<QueryHandlerInterface> shards) {
        // scan all shards for this range and put the Versions into the map
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

        return versions;
    }
}
