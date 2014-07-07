package de.unihamburg.pimpstore.backend;

import junit.framework.TestSuite;

public class AllTests {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite("Backend tests");
        // $JUnit-BEGIN$
        suite.addTestSuite(VersionTest.class);
        suite.addTestSuite(QueryHandlerTest.class);
        // $JUnit-END$
        return suite;
    }

}
