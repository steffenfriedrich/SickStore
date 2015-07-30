package de.unihamburg.sickstore.backend.sharding;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestScan;
import de.unihamburg.sickstore.database.messages.ServerResponseScan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * minium value        s1             s2           maximum value
 *    |                |             |                |
 *    |     SHARD1     |   SHARD2    |      SHARAD3   |
 *
 * The ranges define the inner separators of the shards. Therefore, when there are 3 shards, two
 * separators are needed. The minimum and maximum values are implicit separators and do not need to
 * be defined.
 */
public class RangeBasedStrategy implements ShardingStrategy {

    private String[] rangeSeparators;
    private boolean caseSensitive = true;

    /**
     * @param rangeSeparators see class comment.
     */
    public RangeBasedStrategy(String[] rangeSeparators, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;

        Arrays.sort(rangeSeparators);
        if (!caseSensitive) {
            String[] lowerCase = new String[rangeSeparators.length];
            for (int i = 0; i < rangeSeparators.length; i++) {
                lowerCase[i] = rangeSeparators[i].toLowerCase();
            }

            rangeSeparators = lowerCase;
        }

        this.rangeSeparators = rangeSeparators;
    }

    public RangeBasedStrategy(String[] rangeSeparators) {
        this(rangeSeparators, true);
    }

    /**
     * Determines the target-shard according to the configured ranges.
     * @param request
     * @param shards
     * @return
     */
    @Override
    public QueryHandlerInterface getTargetShard(ClientRequest request,
                                                List<QueryHandlerInterface> shards) {
        if (shards.size() != rangeSeparators.length + 1) {
            throw new RuntimeException("The number of separators does not correspond to the " +
                "number of shards (separators + 1 = shards)");
        }

        String key = request.getKey();
        if (!caseSensitive) {
            key = key.toLowerCase();
        }

        int shard = 0;
        for (String separator : rangeSeparators) {
            if (key.compareTo(separator) <= 0) {
                break;
            }
            shard++;
        }

        return shards.get(shard);
    }

    /**
     * Do a scan request that respects all shards that might contain the data.
     * @param request
     * @param shards
     * @return
     */
    @Override
    public List<Version> doScanRequest(ClientRequestScan request,
                                       List<QueryHandlerInterface> shards) {
        QueryHandlerInterface firstShard = getTargetShard(request, shards);

        int range = request.getRecordcount();
        List<Version> versions = new ArrayList<>(range);
        int i = shards.indexOf(firstShard);
        while (i < shards.size() && request.getRecordcount() > 0) {
            QueryHandlerInterface shard = shards.get(i);
            ServerResponseScan response = (ServerResponseScan) shard.processQuery(request);

            versions.addAll(response.getEntries());

            // reduce the number of needed records
            request.setRecordcount(request.getRecordcount() - response.getEntries().size());

            i++;
        }

        return versions;
    }
}
