package de.unihamburg.sickstore.database.hikari;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import de.unihamburg.sickstore.database.client.Connection;
import de.unihamburg.sickstore.database.client.SickConnection;
import de.unihamburg.sickstore.database.client.SickStoreHikariPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static de.unihamburg.sickstore.database.hikari.ProxyConnection.*;
import static de.unihamburg.sickstore.database.hikari.ProxyConnection.DIRTY_BIT_SCHEMA;
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedNanos;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Reimplemtation of HikariCP's {@link com.zaxxer.hikari.pool.PoolBase}  for the use with SickStore
 * @author Steffen Friedrich
 */
abstract public class PoolBase {
    private final Logger LOGGER = LoggerFactory.getLogger(PoolBase.class);

    public IMetricsTrackerDelegate metricsTracker;

    long connectionTimeout;
    long validationTimeout;

    private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout", "schema"};
    private static final int UNINITIALIZED = -1;
    private static final int TRUE = 1;
    private static final int FALSE = 0;

    private int networkTimeout = UNINITIALIZED;
    private int isNetworkTimeoutSupported = UNINITIALIZED;
    private int isQueryTimeoutSupported = UNINITIALIZED;
    private int defaultTransactionIsolatio = UNINITIALIZED;
    private int transactionIsolation = UNINITIALIZED;
    private final String schema = "";
    private final boolean isReadOnly = false;
    private final boolean isAutoCommit = false;

    private final boolean isUseJdbc4Validation = false;
    private final boolean isIsolateInternalQueries = false;
    private final AtomicReference<Throwable> lastConnectionFailure;

    private volatile boolean isValidChecked;

    public final HikariConfig config;
    private final SickStoreHikariPool client;

    protected volatile String catalog;

    protected final String poolName;

    PoolBase(SickStoreHikariPool client, final HikariConfig config) {
        this.config = config;
        this.lastConnectionFailure = new AtomicReference<>();
        this.client = client;
        this.catalog = config.getCatalog();
        this.poolName = config.getPoolName();
    }

    private SickConnection newConnection() {
        final long start = currentTime();
        SickConnection connection = null;
        try {
            connection = client.getConnectionFactory().open(client.getHost());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return connection;
    }

    abstract void recycle(final PoolEntry poolEntry);

    PoolEntry newPoolEntry() throws Exception {
        return new PoolEntry(newConnection(), this);
    }

    void resetConnectionState(final SickConnection connection, final ProxyConnection proxyConnection, final int dirtyBits) throws SQLException
    {
        int resetBits = 0;

        if ((dirtyBits & DIRTY_BIT_READONLY) != 0 && proxyConnection.getReadOnlyState() != isReadOnly) {
            resetBits |= DIRTY_BIT_READONLY;
        }

        if ((dirtyBits & DIRTY_BIT_AUTOCOMMIT) != 0 && proxyConnection.getAutoCommitState() != isAutoCommit) {
            resetBits |= DIRTY_BIT_AUTOCOMMIT;
        }

        if ((dirtyBits & DIRTY_BIT_ISOLATION) != 0 && proxyConnection.getTransactionIsolationState() != transactionIsolation) {
            resetBits |= DIRTY_BIT_ISOLATION;
        }

        if ((dirtyBits & DIRTY_BIT_CATALOG) != 0 && catalog != null && !catalog.equals(proxyConnection.getCatalogState())) {
            resetBits |= DIRTY_BIT_CATALOG;
        }

        if ((dirtyBits & DIRTY_BIT_NETTIMEOUT) != 0 && proxyConnection.getNetworkTimeoutState() != networkTimeout) {
            resetBits |= DIRTY_BIT_NETTIMEOUT;
        }

        if ((dirtyBits & DIRTY_BIT_SCHEMA) != 0 && schema != null && !schema.equals(proxyConnection.getSchemaState())) {
            resetBits |= DIRTY_BIT_SCHEMA;
        }

        if (resetBits != 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} - Reset ({}) on connection {}", poolName, stringFromResetBits(resetBits), connection);
        }
    }

    void quietlyCloseConnection(final Connection connection, final String closureReason) {
        if (connection != null) {
            try {
                LOGGER.debug("{} - Closing connection {}: {}", poolName, connection, closureReason);
                connection.close(); // continue with the close even if setNetworkTimeout() throws
            } catch (Throwable e) {
                LOGGER.debug("{} - Closing connection {} failed", poolName, connection, e);
            }
        }
    }

    Throwable getLastConnectionFailure() {
        return lastConnectionFailure.get();
    }

    /**
     * Register MBeans for HikariConfig and HikariPool.
     *
     * @param hikariPool a HikariPool instance
     */
    void registerMBeans(final HikariPool hikariPool) {
        if (!config.isRegisterMbeans()) {
            return;
        }

        try {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            final ObjectName beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
            final ObjectName beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
            if (!mBeanServer.isRegistered(beanConfigName)) {
                mBeanServer.registerMBean(config, beanConfigName);
                mBeanServer.registerMBean(hikariPool, beanPoolName);
            } else {
                LOGGER.error("{} - JMX name ({}) is already registered.", poolName, poolName);
            }
        } catch (Exception e) {
            LOGGER.warn("{} - Failed to register management beans.", poolName, e);
        }
    }

    /**
     * Unregister MBeans for HikariConfig and HikariPool.
     */
    void unregisterMBeans() {
        if (!config.isRegisterMbeans()) {
            return;
        }

        try {
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            final ObjectName beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
            final ObjectName beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
            if (mBeanServer.isRegistered(beanConfigName)) {
                mBeanServer.unregisterMBean(beanConfigName);
                mBeanServer.unregisterMBean(beanPoolName);
            }
        } catch (Exception e) {
            LOGGER.warn("{} - Failed to unregister management beans.", poolName, e);
        }
    }

    boolean isConnectionAlive(final SickConnection connection) {
        if (connection.getChannel().isOpen()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This will create a string for debug logging. Given a set of "reset bits", this
     * method will return a concatenated string, for example:
     *
     * Input : 0b00110
     * Output: "autoCommit, isolation"
     *
     * @param bits a set of "reset bits"
     * @return a string of which states were reset
     */
    private String stringFromResetBits(final int bits)
    {
        final StringBuilder sb = new StringBuilder();
        for (int ndx = 0; ndx < RESET_STATES.length; ndx++) {
            if ( (bits & (0b1 << ndx)) != 0) {
                sb.append(RESET_STATES[ndx]).append(", ");
            }
        }

        sb.setLength(sb.length() - 2);  // trim trailing comma
        return sb.toString();
    }

    long getLoginTimeout()
    {
            return SECONDS.toSeconds(5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return poolName;
    }

    static class ConnectionSetupException extends Exception {
        private static final long serialVersionUID = 929872118275916521L;

        ConnectionSetupException(Throwable t) {
            super(t);
        }
    }

    interface IMetricsTrackerDelegate extends AutoCloseable {
        default void recordConnectionUsage(PoolEntry poolEntry) {
        }

        default void recordConnectionCreated(long connectionCreatedMillis) {
        }

        default void recordBorrowTimeoutStats(long startTime) {
        }

        default void recordBorrowStats(final PoolEntry poolEntry, final long startTime) {
        }

        default void recordConnectionTimeout() {
        }

        @Override
        default void close() {
        }
    }

    /**
     * A class that delegates to a MetricsTracker implementation.  The use of a delegate
     * allows us to use the NopMetricsTrackerDelegate when metrics are disabled, which in
     * turn allows the JIT to completely optimize away to callsites to record metrics.
     */
    static class MetricsTrackerDelegate implements PoolBase.IMetricsTrackerDelegate {
        final IMetricsTracker tracker;

        MetricsTrackerDelegate(IMetricsTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public void recordConnectionUsage(final PoolEntry poolEntry) {
            tracker.recordConnectionUsageMillis(poolEntry.getMillisSinceBorrowed());
        }

        @Override
        public void recordConnectionCreated(long connectionCreatedMillis) {
            tracker.recordConnectionCreatedMillis(connectionCreatedMillis);
        }

        @Override
        public void recordBorrowTimeoutStats(long startTime) {
            tracker.recordConnectionAcquiredNanos(elapsedNanos(startTime));
        }

        @Override
        public void recordBorrowStats(final PoolEntry poolEntry, final long startTime) {
            final long now = currentTime();
            poolEntry.lastBorrowed = now;
            tracker.recordConnectionAcquiredNanos(elapsedNanos(startTime, now));
        }

        @Override
        public void recordConnectionTimeout() {
            tracker.recordConnectionTimeout();
        }

        @Override
        public void close() {
            tracker.close();
        }
    }

    /**
     * A no-op implementation of the IMetricsTrackerDelegate that is used when metrics capture is
     * disabled.
     */
    static final class NopMetricsTrackerDelegate implements PoolBase.IMetricsTrackerDelegate {
    }

    public String getPoolName() {
        return poolName;
    }
}
