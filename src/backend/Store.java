/**
 * 
 */
package backend;

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

    private TreeMap<String, Entry> values = new TreeMap<String, Entry>();

    public synchronized Entry get(String key) {
        return values.get(key);
    }

    /**
     * Stores the given entry value under the given key. If the given value is
     * <code>null</code>, the given key is removed.
     * 
     * @param key
     * @param value
     */
    public synchronized void put(String key, Entry value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    /**
     * Updates the given column under the given key with the given value. If
     * there is no entry under the given key, a new entry is created. If the
     * given column is <code>null</code>, it is removed.
     * 
     * @param key
     * @param column
     * @param value
     */
    public synchronized void put(String key, String column, Object value) {
        Entry entry = values.get(key);

        if (entry == null) {
            entry = new Entry();
            put(key, entry);
        }

        entry.put(column, value);
    }

    public synchronized Object get(String key, String column) {
        Entry entry = values.get(key);

        if (entry == null) {
            return null;
        }

        return entry.get(column);
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
