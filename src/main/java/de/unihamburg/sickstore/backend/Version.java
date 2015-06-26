/**
 * 
 */
package de.unihamburg.sickstore.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.SickServer;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Version {

    /** a version representing the initial null value */
    public static final Version NULL = new Version(-1, -1, null, true);

    /** if true, there is no value under the given key in this version */
    private boolean isNull = false;

    private TreeMap<String, Object> values = new TreeMap<String, Object>();

    /**
     * A map from server IDs to timestamps; indicates when the version is
     * visible for what server
     */
    private StalenessMap visibility = new StalenessMap();

    /**
     * Server timestamp at which this version was written.
     * This value always has to be set this item is written
     */
    private long writtenAt = -1;

    /** indicates what server has written the version and owns the data */
    private int writtenBy;

    public Version() {
    }

    public Version(int writtenBy, long writtenAt, StalenessMap visibility) {
        this(writtenBy, writtenAt, visibility, false);
    }

    public Version(int writtenBy, long writtenAt, StalenessMap visibility, boolean isNull) {
        this();
        this.writtenBy = writtenBy;
        if (visibility != null) {
            this.visibility.putAll(visibility);
        }
        this.isNull = isNull;
        this.writtenAt = writtenAt;
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

    public StalenessMap getVisibility() {
        return visibility;
    }

    public long getWrittenAt() {
        if (writtenAt == -1) {
            throw new RuntimeException("writtenAt not set");
        }

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

    public void setVisibility(StalenessMap visibility) {
        this.visibility = visibility;
    }

    public void setWrittenAt(long writtenAt) {
        this.writtenAt = writtenAt;
    }

    @Override
    public String toString() {
        if (isNull) {
            return writtenAt + ": null";
        }
        return writtenAt + ": " + values.toString();
    }
}
