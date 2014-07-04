package database;

import junit.framework.TestSuite;

public class AllTests
{

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite("Database tests");
        // $JUnit-BEGIN$
        suite.addTestSuite(PIMPClientTest.class);
        // $JUnit-END$
        return suite;
    }

}
