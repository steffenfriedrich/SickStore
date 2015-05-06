package de.unihamburg.sickstore.database;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SickClientTest.class,
        SickServerTest.class
})
public class AllTests {
}