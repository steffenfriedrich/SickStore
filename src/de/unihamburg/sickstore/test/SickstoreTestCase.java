package de.unihamburg.sickstore.test;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.AnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.MongoDbClientDelay;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.junit.Before;

public abstract class SickstoreTestCase {

    protected TimeHandler timeHandler;
    protected Store store;
    protected BasicAnomalyGenerator anomalyGenerator;
    protected QueryHandler queryHandler;

    @Before
    public void initEssentials() throws Exception {
        // init store and query handler
        timeHandler = new FakeTimeHandler();
        store = new Store(timeHandler);
        anomalyGenerator = new BasicAnomalyGenerator(
            new ConstantStaleness(500, 0),
            new ZeroClientDelay()
        );

        queryHandler = new QueryHandler(store, anomalyGenerator, timeHandler);
    }
}
