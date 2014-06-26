/**
 * 
 */
package backend;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Entry {
    private Map<String, Object> values = new HashMap<String, Object>(); 

    public Entry() {
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
        this.values = values;
    }

    public synchronized void remove(String column) {
        put(column, null);
    } 
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Entry) {
            Entry otherEntry=(Entry) obj;
            return values.equals(otherEntry.getValues());
        }
        return false;
    }
}
