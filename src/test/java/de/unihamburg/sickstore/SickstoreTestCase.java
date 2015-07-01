package de.unihamburg.sickstore;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.junit.Before;

public abstract class SickstoreTestCase {

    protected TimeHandler timeHandler;
    protected BasicAnomalyGenerator anomalyGenerator;
    protected QueryHandler queryHandler;

    @Before
    public void initEssentials() throws Exception {
        // init store and query handler
        timeHandler = new FakeTimeHandler();
        anomalyGenerator = new BasicAnomalyGenerator(
            new ConstantStaleness(500, 0),
            new ZeroClientDelay()
        );

        Store store = new Store(timeHandler);
        queryHandler = new QueryHandler(store, anomalyGenerator, timeHandler);
    }
}
