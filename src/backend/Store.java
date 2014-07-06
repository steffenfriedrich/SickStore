/**
 * 
 */
package backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import database.messages.exception.DeleteException;
import database.messages.exception.InsertException;
import database.messages.exception.UpdateException;

/**
 * 
 * This class is responsible for introducing data-centric staleness by serving
 * stale data to the server nodes. </br> To this end, all servers have to supply
 * the {@link Store} instance with a reference to themselves, so that they get
 * their corresponding degree of staleness.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class Store {
    private final static Store instance;

    static {
        instance = new Store();
    }

    public static Store getInstance() {
        return instance;
    }

    private TreeMap<String, VersionSet> values = new TreeMap<String, VersionSet>();

    private Store() {
    }

    public void delete(int server, String key, Version version, long timestamp)
            throws DeleteException {
        Version alreadyExisting = get(server, key, timestamp);
        if (alreadyExisting.isNull()) {
            throw new DeleteException(
                    "Value cannot be deleted, because there is no value under key \""
                            + key + "\".");
        } else {
            insertOrUpdate(key, version, timestamp);
        }
    }

    public synchronized Version get(int server, String key, long timestamp) {
        return get(server, key, (Set<String>) null, timestamp);
    }

    public synchronized Version get(int server, String key,
            Set<String> columns, long timestamp) {
        if (key == null) {
            throw new NullPointerException("Key must not be null!");
        }

        VersionSet versions = getVersionSet(key);
        Version version = Version.NULL;

        // find the version that was up-to-date (most recent) at the given
        // timestamp
        if (versions != null) {
            NavigableSet<Long> timestamps = versions.descendingKeySet();
            for (long t : timestamps) {
                version = versions.get(t);
                if (isVisible(server, version.getVisibility(), timestamp, t)) {
                    break;
                } else {
                    version = Version.NULL;
                }
            }
        }

        if (!version.isNull()) {
            try {
                return version.clone(columns);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return Version.NULL;
    }

    public synchronized Version get(int server, String key, String column,
            long timestamp) {
        if (column == null) {
            throw new IllegalArgumentException("Column must not be null!");
        }
        Set<String> columns = new HashSet<String>();
        columns.add(column);
        return get(server, key, columns, timestamp);
    }

    public synchronized List<Version> getRange(int server, String key,
            int range, boolean asc, Set<String> columns, long timestamp) {
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
                && (asc && (nextKey = higherKey(nextKey)) != null || !asc
                        && (nextKey = lowerKey(nextKey)) != null));

        return versions;
    }

    private synchronized VersionSet getVersionSet(String key) {
        return values.get(key);
    }

    /**
     * Returns the least key strictly greater than the given key, or null if
     * there is no such key.
     * 
     * 
     * @param key
     * @return
     */
    private synchronized String higherKey(String key) {
        return values.higherKey(key);
    }

    public void insert(int server, String key, Version version, long timestamp)
            throws InsertException {
        Version alreadyExisting = get(server, key, timestamp);
        if (alreadyExisting.isNull()) {
            insertOrUpdate(key, version, timestamp);
        } else {
            throw new InsertException(
                    "Value cannot be stored, because there already is a value under key \""
                            + key + "\".");
        }
    }

    /**
     * Stores the given entry value under the given key and timestamp.
     * 
     * @param key
     * @param value
     * @param timestamp
     */
    public synchronized void insertOrUpdate(String key, Version value,
            Long timestamp) {
        VersionSet entrySet = values.get(key);

        if (entrySet == null) {
            entrySet = new VersionSet();

            values.put(key, entrySet);
        }
        if (entrySet.get(timestamp) != null) {
            throw new IllegalArgumentException(
                    "Value cannot be written under key \""
                            + key
                            + "\", because there already was a value with the exact same timestamp ("
                            + timestamp + ").");
        }
        entrySet.put(timestamp, value);
    }

    /**
     * 
     * @param server
     *            a server ID
     * @param stalenessWindows
     * @param readTimestamp
     * @param writeTimestamp
     * @return
     */
    public boolean isVisible(int server, Map<Integer, Long> stalenessWindows,
            long readTimestamp, long writeTimestamp) {
        Long staleness = stalenessWindows.get(server);
        if (staleness == null) {
            return false;
        } else {
            long visibleSince = writeTimestamp + staleness;

            return visibleSince <= readTimestamp;
        }
    }

    /**
     * Returns the greatest key strictly less than the given key, or null if
     * there is no such key.
     * 
     * 
     * @param key
     * @return
     */
    private synchronized String lowerKey(String key) {
        return values.lowerKey(key);
    }

    public void update(int server, String key, Version version, long timestamp)
            throws UpdateException {
        Version alreadyExisting = get(server, key, timestamp);
        if (alreadyExisting.isNull()) {
            throw new UpdateException(
                    "Value cannot be updated, because there is no value under key \""
                            + key + "\".");
        } else {
            insertOrUpdate(key, version, timestamp);
        }
    }
}
