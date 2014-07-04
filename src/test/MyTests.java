package test;

import java.io.IOException;
import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <p>
 * The main test class for this application. Executes all tests.
 * </p>
 * 
 * @author <a href="7friedri@informatik.uni-hamburg.de">Steffen Friedrich</a>
 *         and <a href="7wingera@informatik.uni-hamburg.de">Wolfram
 *         Wingerath</a>
 * 
 */
public class MyTests extends TestSuite
{ 
    
    public static TestSuite suite() throws IOException
    { 
        MyTests suite = new MyTests();
        suite.setName("All tests");
        // $JUnit-BEGIN$
        suite.addTests(backend.AllTests.suite());
        suite.addTests(database.AllTests.suite());
        // $JUnit-END$
        return suite;
    }
 

    /**
     * <p>
     * Adds all tests of the given TestSuite <code>suite</code> to the instance
     * on which this method was invoked.
     * </p>
     * 
     * @.lastchange 29.08.2009
     * @param suite
     *            the <code>TestSuite</code> that "contains" the tests to be
     *            added
     */
    private void addTests(TestSuite suite)
    {
        Enumeration<Test> tests = suite.tests();
        while (tests.hasMoreElements())
        {
            this.addTest(tests.nextElement());
        }
    }
}
