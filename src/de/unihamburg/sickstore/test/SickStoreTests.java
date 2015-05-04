package de.unihamburg.sickstore.test;


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
import org.junit.runners.Suite;
import org.junit.runner.RunWith;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    de.unihamburg.sickstore.backend.AllTests.class,
    de.unihamburg.sickstore.database.AllTests.class
})
public class SickStoreTests {
}
