package de.unihamburg.sickstore.backend;

import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;

import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.database.Node;
import org.junit.Test;

public class VersionTest {

    @Test
    public void testClone() throws CloneNotSupportedException {

        // Create visibility map
        StalenessMap visibility = new StalenessMap();
        visibility.put(new Node("1"), 100l);
        visibility.put(new Node("2"), 102l);
        // create a version object and set some values
        Version version = new Version();
        version.setVisibility(visibility);
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
        assertNotSame(version.getVisibility(), clone.getVisibility());
        assertEquals(version.getWrittenBy(), clone.getWrittenBy());
        assertEquals(version.get("name"), clone.get("name"));
        assertSame(version.get("name"), clone.get("name"));
        assertNull(clone.get("age"));
        System.out.println(clone);
    }

}
