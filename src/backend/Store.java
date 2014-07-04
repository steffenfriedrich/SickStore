/**
 * 
 */
package backend;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Store {

    final static Store instance;

    static {
        instance = new Store();
    }

    private Store() {
    }

    public static Store getInstance() {
        return instance;
    }

    private TreeMap<String, VersionSet> values = new TreeMap<String, VersionSet>();

    public synchronized VersionSet get(String key) {
        return values.get(key);
    }

    /**
     * Stores the given entry value under the given key and timestamp.
     * 
     * @param key
     * @param value
     * @param timestamp
     */
    public synchronized void put(String key, Version value, Long timestamp) {
        VersionSet entrySet = values.get(key);

        if (entrySet == null) {
            entrySet = new VersionSet();

            values.put(key, entrySet);
        }
        entrySet.put(timestamp, value);
    }

    /**
     * Returns a list of all the keys in a specific key range
     * 
     * @param key
     *            the first key in the range
     * @param range
     *            that is a number of keys (key range size)
     * @param asc
     *            if true, the range consists of <code>key</code>and a
     *            higher-valued keys; else, the range consists of
     *            <code>key</code> and lower-valued keys
     * @return a list of all the keys in a specific key range
     */
    public synchronized List<String> getKeyRange(String key, int range,
            final boolean asc) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null!");
        }
        if (range < 1) {
            throw new IllegalArgumentException(
                    "Range must be greater than or equal to 1!");
        }
        String nextKey = key;
        List<String> versions = new ArrayList<String>(range);
        versions.add(key);

        while (range > 1
                && (asc && (nextKey = higherKey(nextKey)) != null || !asc
                        && (nextKey = lowerKey(nextKey)) != null)) {
            versions.add(nextKey);
            range--;
        }

        return versions;
    }

    /**
     * Returns a list of all the {@link VersionSet} instances in a specific key
     * range
     * 
     * @return a list of all the {@link VersionSet} instances in a specific key
     *         range
     * 
     * @see #getKeyRange(String, int, boolean)
     */
    public synchronized List<VersionSet> getRange(String key, int range,
            final boolean asc) {
        List<VersionSet> versions = new ArrayList<VersionSet>(range);

        for (String k : getKeyRange(key, range, asc)) {
            versions.add(get(k));
        }

        return versions;
    }

    /**
     * Returns the least key strictly greater than the given key, or null if
     * there is no such key.
     * 
     * 
     * @param key
     * @return
     */
    public synchronized String higherKey(String key) {
        return values.higherKey(key);
    }

    /**
     * Returns the greatest key strictly less than the given key, or null if
     * there is no such key.
     * 
     * 
     * @param key
     * @return
     */
    public synchronized String lowerKey(String key) {
        return values.lowerKey(key);
    }
}
