/**
 * 
 */
package backend;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import database.PIMPServer;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Version {
    /** indicates what server has written the version */
    private int writtenBy;

    /** if true, there is no value under the given key in this version */
    private boolean isNull = false;

    /**
     * a map from server IDs to timestamps; indicates when the version is
     * visible for what server
     */
    private Map<Integer, Long> visibility;

    public Map<Integer, Long> getVisibility() {
        return visibility;
    }

    public int getWrittenBy() {
        return writtenBy;
    }

    private TreeMap<String, Object> values = new TreeMap<String, Object>();

    public Version(int writtenBy, Map<Integer, Long> visibility) {
        this(writtenBy, visibility, false);
    }

    public Version(int writtenBy, Map<Integer, Long> visibility, boolean isNull) {
        this();
        this.writtenBy = writtenBy;
        this.visibility = visibility;
        this.isNull = isNull;
    }

    /** a version representing the null value */
    public static final Version NULL = new Version(-1, null, true);

    public Version(PIMPServer server, Map<Integer, Long> visibility) {
        this(server.getID(), visibility);
    }

    public Version() {
    }

    public Object get(String column) {
        return values.get(column);
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

    @Override
    public String toString() {
        return values.toString();
    }

    public synchronized Map<String, Object> getValues() {
        return values;
    }

    public synchronized void setValues(Map<String, Object> values) {
        this.values.clear();
        this.values.putAll(values);
    }

    public synchronized void remove(String column) {
        put(column, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version) {
            Version otherEntry = (Version) obj;
            return values.equals(otherEntry.getValues());
        }
        return false;
    }

    public boolean isNull() {
        return getValues().isEmpty() || isNull;
    }

    public void setVisibility(Map<Integer, Long> visibility) {
        this.visibility = visibility;
    }

    public Version clone(Set<String> columns) throws CloneNotSupportedException {
        Version clone = new Version(writtenBy, visibility);

        for (String column : values.keySet()) {
            if (columns == null || columns.contains(column)) {
                clone.put(column, get(column));
            }
        }
        return clone;
    }

    @Override
    protected Version clone() throws CloneNotSupportedException {
        return clone(null);
    }

    public static void main(String[] args) throws Exception {
    }
}
