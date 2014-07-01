/**
 * 
 */
package backend;

import java.util.TreeMap;

/**
 * @author Wolfram Wingerath
 * 
 */
public class EntrySet {

    private TreeMap<Long, Entry> versions = new TreeMap<Long, Entry>();

    public Entry put(Long timestamp, Entry entry) {
       return versions.put(timestamp, entry);
    }

    public Entry get(Long timestamp) {
       return versions.get(timestamp);
    }
    
    public Entry remove(Long timestamp) {
        return versions.remove(timestamp);
    }
}
