/**
 * 
 */
package de.unihamburg.pimpstore.backend;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.unihamburg.pimpstore.database.PIMPServer;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Version {

    /** a version representing the initial null value */
    public static final Version NULL = new Version(-1, null, true);

    /** if true, there is no value under the given key in this version */
    private boolean isNull = false;

    private TreeMap<String, Object> values = new TreeMap<String, Object>();

    /**
     * a map from server IDs to timestamps; indicates when the version is
     * visible for what server
     */
    private Map<Integer, Long> visibility;

    /**
     * server timestamp at which this version was written. Default value is the
     * current system time. Value <code>Long.MIN_VALUE</code> indicates this
     * value was initially there
     */
    private long writtenAt = System.currentTimeMillis();

    /** indicates what server has written the version */
    private int writtenBy;

    public Version() {
    }

    public Version(int writtenBy, long writtenAt, Map<Integer, Long> visibility) {
        this(writtenBy, visibility, false);
    }

    public Version(int writtenBy, Map<Integer, Long> visibility, boolean isNull) {
        this();
        this.writtenBy = writtenBy;
        this.visibility = visibility;
        this.isNull = isNull;
    }

    public Version(PIMPServer server, long writtenAt,
            Map<Integer, Long> visibility) {
        this(server.getID(), writtenAt, visibility);
    }

    @Override
    protected Version clone() throws CloneNotSupportedException {
        return clone(null);
    }

    public Version clone(Set<String> columns) throws CloneNotSupportedException {
        Version clone = new Version(writtenBy, writtenAt, visibility);

        for (String column : values.keySet()) {
            if (columns == null || columns.contains(column)) {
                clone.put(column, get(column));
            }
        }
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version) {
            Version otherEntry = (Version) obj;
            return values.equals(otherEntry.getValues());
        }
        return false;
    }

    public Object get(String column) {
        return values.get(column);
    }

    public synchronized Map<String, Object> getValues() {
        return values;
    }

    public Map<Integer, Long> getVisibility() {
        return visibility;
    }

    public long getWrittenAt() {
        return writtenAt;
    }

    public int getWrittenBy() {
        return writtenBy;
    }

    public boolean isNull() {
        return getValues().isEmpty() || isNull;
    }

    /**
     * 
     * Updates the given column under the given key with the given value. If the
     * given column is <code>null</code>, it is removed.
     * 
     * @param column
     * @param value
     */
    public synchronized void put(String column, Object value) {
        if (column == null) {
            values.remove(value);
        } else {
            values.put(column, value);
        }
    }

    public synchronized void remove(String column) {
        put(column, null);
    }

    public synchronized void setValues(Map<String, Object> values) {
        this.values.clear();
        this.values.putAll(values);
    }

    public void setVisibility(Map<Integer, Long> visibility) {
        this.visibility = visibility;
    }

    public void setWrittenAt(long writtenAt) {
        this.writtenAt = writtenAt;
    }

    @Override
    public String toString() {
        if (isNull) {
            return writtenAt+ ": null";
        }
        return writtenAt+ ": "+values.toString();
    }
}
