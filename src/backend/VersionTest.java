package backend;

import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

public class VersionTest extends TestCase {

    @Test
    public void testClone() throws CloneNotSupportedException {

        // Create visibility map
        Map<Integer, Long> visibility = new HashMap<Integer, Long>();
        visibility.put(1, 100l);
        visibility.put(2, 102l);
        // create a version object and set some values
        Version version = new Version();
        version.put("name", "Bob");
        version.put("age", "25");

        // now clone only a subset of columns
        Set<String> columns = new HashSet<String>();
        columns.add("name");
        Version clone = version.clone(columns);

        // Compare
        assertNotSame(version, clone);
        assertNotEquals(version, clone);
        assertEquals(version.getVisibility(), clone.getVisibility());
        assertNotSame(version.getVisibility(), clone.getValues());
        assertEquals(version.getWrittenBy(), clone.getWrittenBy());
        assertEquals(version.get("name"), clone.get("name"));
        assertSame(version.get("name"), clone.get("name"));
        assertNull(clone.get("age"));
        System.out.println(clone);
    }

}
