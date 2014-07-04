/**
 * 
 */
package backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

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

    public Version get(int server, String key, long timestamp) {
        return get(server, key, (Set<String>) null, timestamp);
    }

    public Version get(int server, String key, Set<String> columns,
            long timestamp) {
        if (key == null) {
            throw new NullPointerException("Key must not be null!");
        }

        VersionSet versions = store.get(key);
        Version version = null;

        // find the version that was up-to-date (most recent) at the given
        // timestamp
        if (versions != null) {
            NavigableSet<Long> timestamps = versions.descendingKeySet();
            for (long t : timestamps) {
                version = versions.get(t);
                if (version.isVisible(server, timestamp)) {
                    break;
                }
            }
        }

        if (version != null) {
            try {
                return version.clone(columns);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return Version.NULL;
    }

    public Version get(int server, String key, String column, long timestamp) {
        if (column == null) {
            throw new IllegalArgumentException("Column must not be null!");
        }
        Set<String> columns = new HashSet<String>();
        columns.add(column);
        return get(server, key, columns, timestamp);
    }

    public List<Version> getRange(int server, String key, int range,
            boolean asc, Set<String> columns, long timestamp) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null!");
        }
        if (range < 1) {
            throw new IllegalArgumentException(
                    "Range must be greater than or equal to 1!");
        }

        List<Version> versions = new ArrayList<Version>(range);
        String nextKey = key;
        Version version = null;

        do {
            version = get(server, nextKey, columns, timestamp);
            if (!version.isNull()) {
                versions.add(version);
            }
        } while (range > versions.size()
                && (asc && (nextKey = store.higherKey(nextKey)) != null || !asc
                        && (nextKey = store.lowerKey(nextKey)) != null));

        return versions;
    }

    public void put(String key, Version version, long timestamp) {
        store.put(key, version, timestamp);
    }
}
