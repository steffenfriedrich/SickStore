package de.unihamburg.sickstore.test;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.junit.Before;

public abstract class SickstoreTestCase {

    protected TimeHandler timeHandler;
    protected Store store;
    protected QueryHandler queryHandler;

    @Before
    public void initEssentials() throws Exception {
        // init store and query handler
        timeHandler = new FakeTimeHandler();
        store = new Store(timeHandler);
        queryHandler = new QueryHandler(store, new ConstantStaleness(500, 0), timeHandler);
    }
}
