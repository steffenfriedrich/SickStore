package de.unihamburg.sickstore.database;

import junit.framework.TestSuite;

public class AllTests {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite("Database tests");
        // $JUnit-BEGIN$
        suite.addTestSuite(SickClientTest.class);
        suite.addTestSuite(SickServerTest.class);
        // $JUnit-END$
        return suite;
    }

}
