package de.unihamburg.sickstore.database.hikari;

import com.codahale.metrics.MetricRegistry;

import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import com.zaxxer.hikari.metrics.dropwizard.CodahaleMetricsTrackerFactory;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;
import com.zaxxer.hikari.util.SuspendResumeLock;
import de.unihamburg.sickstore.database.client.Connection;
import com.codahale.metrics.health.HealthCheckRegistry;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static com.zaxxer.hikari.util.ClockSource.*;
import static com.zaxxer.hikari.util.ClockSource.elapsedDisplayString;
import static com.zaxxer.hikari.util.ClockSource.plusMillis;
import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static com.zaxxer.hikari.util.UtilityElf.safeIsAssignableFrom;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import de.unihamburg.sickstore.database.client.SickStoreHikariPoolClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reimplemtation of HikariCP's {@link com.zaxxer.hikari.pool.HikariPool}  for the use with SickStore
 * @author Steffen Friedrich
 */
public class HikariPool extends PoolBase implements HikariPoolMXBean, IBagStateListener {
    private final Logger LOGGER = LoggerFactory.getLogger(com.zaxxer.hikari.pool.HikariPool.class);

    public static final int POOL_NORMAL = 0;
    public static final int POOL_SUSPENDED = 1;
    public static final int POOL_SHUTDOWN = 2;
    public volatile int poolState;
    private final long ALIVE_BYPASS_WINDOW_MS = Long.getLong("aliveBypassWindowMs", MILLISECONDS.toMillis(500));
    private final long HOUSEKEEPING_PERIOD_MS = Long.getLong("housekeeping.periodMs", SECONDS.toMillis(30));

    private static final String EVICTED_CONNECTION_MESSAGE = "(connection was evicted)";
    private static final String DEAD_CONNECTION_MESSAGE = "(connection is dead)";
    private final PoolEntryCreator POOL_ENTRY_CREATOR = new PoolEntryCreator(null /*logging prefix*/);
    private final PoolEntryCreator POST_FILL_POOL_ENTRY_CREATOR = new PoolEntryCreator("After adding ");

    private final Collection<Runnable> addConnectionQueue;
    private final ThreadPoolExecutor addConnectionExecutor;
    private final ThreadPoolExecutor closeConnectionExecutor;

    private final ConcurrentBag<PoolEntry> connectionBag;

    private final ProxyLeakTaskFactory leakTaskFactory;
    private final SuspendResumeLock suspendResumeLock;

    private final ScheduledExecutorService houseKeepingExecutorService;
    private ScheduledFuture<?> houseKeeperTask;

    public HikariPool(SickStoreHikariPoolClient client, final HikariConfig config) {
        super(client, config);
        this.connectionBag = new ConcurrentBag<>(this);
        this.suspendResumeLock = config.isAllowPoolSuspension() ? new SuspendResumeLock() : SuspendResumeLock.FAUX_LOCK;
        this.houseKeepingExecutorService = initializeHouseKeepingExecutorService();
        checkFailFast();

        if (config.getMetricsTrackerFactory() != null) {
            setMetricsTrackerFactory(config.getMetricsTrackerFactory());
        }
        else {
            setMetricRegistry(config.getMetricRegistry());
        }
        setHealthCheckRegistry(config.getHealthCheckRegistry());
        registerMBeans(this);

        ThreadFactory threadFactory = config.getThreadFactory();
        LinkedBlockingQueue<Runnable> addConnectionQueue = new LinkedBlockingQueue<>(config.getMaximumPoolSize());
        this.addConnectionQueue = unmodifiableCollection(addConnectionQueue);
        this.addConnectionExecutor = createThreadPoolExecutor(addConnectionQueue, poolName + " connection adder", threadFactory, new ThreadPoolExecutor.DiscardPolicy());
        this.closeConnectionExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), poolName + " connection closer", threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        this.leakTaskFactory = new ProxyLeakTaskFactory(config.getLeakDetectionThreshold(), houseKeepingExecutorService);
        this.houseKeeperTask = houseKeepingExecutorService.scheduleWithFixedDelay(new HouseKeeper(), 100L, HOUSEKEEPING_PERIOD_MS, MILLISECONDS);

        if (Boolean.getBoolean("com.zaxxer.hikari.blockUntilFilled") && config.getInitializationFailTimeout() > 1) {
            final long startTime = currentTime();
            while (elapsedMillis(startTime) < config.getInitializationFailTimeout() && getTotalConnections() < config.getMinimumIdle()) {
                quietlySleep(MILLISECONDS.toMillis(100));
            }
        }
    }

    /**
     * Get a connection from the pool, or timeout after connectionTimeout milliseconds.
     *
     * @return a java.sql.Connection instance
     * @throws SQLException thrown if a timeout occurs trying to obtain a connection
     */
    public Connection getConnection() throws SQLException
    {
        return getConnection(connectionTimeout);
    }

    /**
     * Get a connection from the pool, or timeout after the specified number of milliseconds.
     *
     * @param hardTimeout the maximum time to wait for a connection from the pool
     * @return a java.sql.Connection instance
     * @throws SQLException thrown if a timeout occurs trying to obtain a connection
     */
    public Connection getConnection(final long hardTimeout) throws SQLException
    {
        suspendResumeLock.acquire();
        final long startTime = currentTime();

        try {
            long timeout = hardTimeout;
            do {
                PoolEntry poolEntry = connectionBag.borrow(timeout, MILLISECONDS);
                if (poolEntry == null) {
                    break; // We timed out... break and throw exception
                }

                final long now = currentTime();
                if (poolEntry.isMarkedEvicted() || (elapsedMillis(poolEntry.lastAccessed, now) > ALIVE_BYPASS_WINDOW_MS && !isConnectionAlive(poolEntry.connection))) {
                    closeConnection(poolEntry, poolEntry.isMarkedEvicted() ? EVICTED_CONNECTION_MESSAGE : DEAD_CONNECTION_MESSAGE);
                    timeout = hardTimeout - elapsedMillis(startTime);
                }
                else {
                    metricsTracker.recordBorrowStats(poolEntry, startTime);
                    return poolEntry.connection;
                }
            } while (timeout > 0L);

            metricsTracker.recordBorrowTimeoutStats(startTime);
            throw createTimeoutException(startTime);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException(poolName + " - Interrupted during connection acquisition", e);
        }
        finally {
            suspendResumeLock.release();
        }
    }

    /**
     * Shutdown the pool, closing all idle connections and aborting or closing
     * active connections.
     *
     * @throws InterruptedException thrown if the thread is interrupted during shutdown
     */
    public synchronized void shutdown() throws InterruptedException
    {
        try {
            poolState = POOL_SHUTDOWN;

            if (addConnectionExecutor == null) { // pool never started
                return;
            }

            logPoolState("Before shutdown ");

            if (houseKeeperTask != null) {
                houseKeeperTask.cancel(false);
                houseKeeperTask = null;
            }

            softEvictConnections();

            addConnectionExecutor.shutdown();
            addConnectionExecutor.awaitTermination(getLoginTimeout(), SECONDS);

            destroyHouseKeepingExecutorService();

            connectionBag.close();

            final ExecutorService assassinExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), poolName + " connection assassinator",
                    config.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
            try {
                final long start = currentTime();
                do {
                    abortActiveConnections(assassinExecutor);
                    softEvictConnections();
                } while (getTotalConnections() > 0 && elapsedMillis(start) < SECONDS.toMillis(10));
            }
            finally {
                assassinExecutor.shutdown();
                assassinExecutor.awaitTermination(10L, SECONDS);
            }
            closeConnectionExecutor.shutdown();
            closeConnectionExecutor.awaitTermination(10L, SECONDS);
        }
        finally {
            logPoolState("After shutdown ");
            unregisterMBeans();
            metricsTracker.close();
        }
    }

    /**
     * Attempt to abort or close active connections.
     *
     * @param assassinExecutor the ExecutorService to pass to Connection.abort()
     */
    private void abortActiveConnections(final ExecutorService assassinExecutor)
    {
        for (PoolEntry poolEntry : connectionBag.values(STATE_IN_USE)) {
            Connection connection = poolEntry.close();
            try {
                quietlyCloseConnection(connection, "(connection aborted during shutdown)");
            }
            finally {
                connectionBag.remove(poolEntry);
            }
        }
    }

    /**
     * Create/initialize the Housekeeping service {@link ScheduledExecutorService}.  C create
     * an Executor and configure it.
     *
     * @return either the user specified {@link ScheduledExecutorService}, or the one we created
     */
    private ScheduledExecutorService initializeHouseKeepingExecutorService()
    {
        final ThreadFactory threadFactory = new DefaultThreadFactory(poolName + " housekeeper", true);
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
    private void destroyHouseKeepingExecutorService()
    {
       houseKeepingExecutorService.shutdownNow();
    }

    private void checkFailFast() {
        final long initializationTimeout = config.getInitializationFailTimeout();
        if (initializationTimeout < 0) {
            return;
        }
        final long startTime = currentTime();
        do {
            final PoolEntry poolEntry = createPoolEntry();
            if (poolEntry != null) {
                if (config.getMinimumIdle() > 0) {
                    connectionBag.add(poolEntry);
                }
                else {
                    quietlyCloseConnection(poolEntry.close(), "(initialization check complete and minimumIdle is zero)");
                }

                return;
            }

            if (getLastConnectionFailure() instanceof ConnectionSetupException) {
                throwPoolInitializationException(getLastConnectionFailure().getCause());
            }
            quietlySleep(SECONDS.toMillis(1));
        } while (elapsedMillis(startTime) < initializationTimeout);
        if (initializationTimeout > 0) {
            throwPoolInitializationException(getLastConnectionFailure());
        }
    }

    private void throwPoolInitializationException(Throwable t)
    {
        LOGGER.error("{} - Exception during pool initialization.", poolName, t);
        destroyHouseKeepingExecutorService();
        throw new PoolInitializationException(t);
    }


    private boolean softEvictConnection(final PoolEntry poolEntry, final String reason, final boolean owner)
    {
        poolEntry.markEvicted();
        if (owner || connectionBag.reserve(poolEntry)) {
            closeConnection(poolEntry, reason);
            return true;
        }

        return false;
    }

    /**
     * Creating new poolEntry.  If maxLifetime is configured, create a future End-of-life task with 2.5% variance from
     * the maxLifetime time to ensure there is no massive die-off of Connections in the pool.
     */
    private PoolEntry createPoolEntry() {
        try {
            final PoolEntry poolEntry = newPoolEntry();
            final long maxLifetime = config.getMaxLifetime();
            // variance up to 2.5% of the maxlifetime
            final long variance = maxLifetime > 10_000 ? ThreadLocalRandom.current().nextLong( maxLifetime / 40 ) : 0;
            final long lifetime = maxLifetime - variance;
            poolEntry.setFutureEol(houseKeepingExecutorService.schedule(
                    () -> {
                        if (softEvictConnection(poolEntry, "(connection has passed maxLifetime)", false /* not owner */)) {
                            addBagItem(connectionBag.getWaitingThreadCount());
                        }
                    },
                    lifetime, MILLISECONDS));

            return poolEntry;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Permanently close the real (underlying) connection (eat any exception).
     *
     * @param poolEntry poolEntry having the connection to close
     * @param closureReason reason to close
     */
    void closeConnection(final PoolEntry poolEntry, final String closureReason)
    {
        if (connectionBag.remove(poolEntry)) {
            final Connection connection = poolEntry.close();
            closeConnectionExecutor.execute(() -> {
                quietlyCloseConnection(connection, closureReason);
                if (poolState == POOL_NORMAL) {
                    fillPool();
                }
            });
        }
    }

    /**
     * Fill pool up from current idle connections (as they are perceived at the point of execution) to minimumIdle connections.
     */
    private synchronized void fillPool()
    {
        final int connectionsToAdd = Math.min(config.getMaximumPoolSize() - getTotalConnections(), config.getMinimumIdle() - getIdleConnections())
                - addConnectionQueue.size();
        for (int i = 0; i < connectionsToAdd; i++) {
            addConnectionExecutor.submit((i < connectionsToAdd - 1) ? POOL_ENTRY_CREATOR : POST_FILL_POOL_ENTRY_CREATOR);
        }
    }

    public void recycle(PoolEntry poolEntry) {
    }

    /** {@inheritDoc} */
    @Override
    public int getIdleConnections()
    {
        return connectionBag.getCount(STATE_NOT_IN_USE);
    }

    /** {@inheritDoc} */
    @Override
    public int getTotalConnections()
    {
        return connectionBag.size();
    }


    public static class PoolInitializationException extends RuntimeException
    {
        private static final long serialVersionUID = 929872118275916520L;

        /**
         * Construct an exception, possibly wrapping the provided Throwable as the cause.
         * @param t the Throwable to wrap
         */
        public PoolInitializationException(Throwable t)
        {
            super("Failed to initialize pool: " + t.getMessage(), t);
        }
    }

    /**
     * Log the current pool state at debug level.
     *
     * @param prefix an optional prefix to prepend the log message
     */
    void logPoolState(String... prefix)
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} - {}stats (total={}, active={}, idle={}, waiting={})",
                    poolName, (prefix.length > 0 ? prefix[0] : ""),
                    getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
        }
    }

    /**
     * Set a metrics registry to be used when registering metrics collectors.  The HikariDataSource prevents this
     * method from being called more than once.
     *
     * @param metricRegistry the metrics registry instance to use
     */
    public void setMetricRegistry(Object metricRegistry)
    {
        if (metricRegistry != null && safeIsAssignableFrom(metricRegistry, "com.codahale.metrics.MetricRegistry")) {
            setMetricsTrackerFactory(new CodahaleMetricsTrackerFactory((MetricRegistry) metricRegistry));
        }
        else if (metricRegistry != null && safeIsAssignableFrom(metricRegistry, "io.micrometer.core.instrument.MeterRegistry")) {
            setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory((MeterRegistry) metricRegistry));
        }
        else {
            setMetricsTrackerFactory(null);
        }
    }

    /**
     * Set the MetricsTrackerFactory to be used to create the IMetricsTracker instance used by the pool.
     *
     * @param metricsTrackerFactory an instance of a class that subclasses MetricsTrackerFactory
     */
    public void setMetricsTrackerFactory(MetricsTrackerFactory metricsTrackerFactory)
    {
        if (metricsTrackerFactory != null) {
            this.metricsTracker = new PoolBase.MetricsTrackerDelegate(metricsTrackerFactory.create(config.getPoolName(), getPoolStats()));
        }
        else {
            this.metricsTracker = new PoolBase.NopMetricsTrackerDelegate();
        }
    }

    /**
     * Set the health check registry to be used when registering health checks.  Currently only Codahale health
     * checks are supported.
     *
     * @param healthCheckRegistry the health check registry instance to use
     */
    public void setHealthCheckRegistry(Object healthCheckRegistry)
    {
        if (healthCheckRegistry != null) {
            CodahaleHealthChecker.registerHealthChecks(this, config, (HealthCheckRegistry) healthCheckRegistry);
        }
    }

    /**
     * Create a timeout exception (specifically, {@link SQLTransientConnectionException}) to be thrown, because a
     * timeout occurred when trying to acquire a Connection from the pool.  If there was an underlying cause for the
     * timeout, e.g. a SQLException thrown by the driver while trying to create a new Connection, then use the
     * SQL State from that exception as our own and additionally set that exception as the "next" SQLException inside
     * of our exception.
     *
     * As a side-effect, log the timeout failure at DEBUG, and record the timeout failure in the metrics tracker.
     *
     * @param startTime the start time (timestamp) of the acquisition attempt
     * @return a SQLException to be thrown from {@link #getConnection()}
     */
    private SQLException createTimeoutException(long startTime)
    {
        logPoolState("Timeout failure ");
        metricsTracker.recordConnectionTimeout();

        String sqlState = null;
        final Throwable originalException = getLastConnectionFailure();
        if (originalException instanceof SQLException) {
            sqlState = ((SQLException) originalException).getSQLState();
        }
        final SQLException connectionException = new SQLTransientConnectionException(poolName + " - Connection is not available, request timed out after " + elapsedMillis(startTime) + "ms.", sqlState, originalException);
        if (originalException instanceof SQLException) {
            connectionException.setNextException((SQLException) originalException);
        }

        return connectionException;
    }


    // ***********************************************************************
    //                        IBagStateListener callback
    // ***********************************************************************

    /** {@inheritDoc} */
    @Override
    public void addBagItem(final int waiting)
    {
        final boolean shouldAdd = waiting - addConnectionQueue.size() >= 0; // Yes, >= is intentional.
        if (shouldAdd) {
            addConnectionExecutor.submit(POOL_ENTRY_CREATOR);
        }
    }

    // ***********************************************************************
    //                        HikariPoolMBean methods
    // ***********************************************************************

    /** {@inheritDoc} */
    @Override
    public int getActiveConnections()
    {
        return connectionBag.getCount(STATE_IN_USE);
    }

    /** {@inheritDoc} */
    @Override
    public int getThreadsAwaitingConnection()
    {
        return connectionBag.getWaitingThreadCount();
    }

    /** {@inheritDoc} */
    @Override
    public void softEvictConnections()
    {
        connectionBag.values().forEach(poolEntry -> softEvictConnection(poolEntry, "(connection evicted)", false /* not owner */));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void suspendPool()
    {
        if (suspendResumeLock == SuspendResumeLock.FAUX_LOCK) {
            throw new IllegalStateException(poolName + " - is not suspendable");
        }
        else if (poolState != POOL_SUSPENDED) {
            suspendResumeLock.suspend();
            poolState = POOL_SUSPENDED;
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void resumePool()
    {
        if (poolState == POOL_SUSPENDED) {
            poolState = POOL_NORMAL;
            fillPool();
            suspendResumeLock.resume();
        }
    }
    /**
     * Create a PoolStats instance that will be used by metrics tracking, with a pollable resolution of 1 second.
     *
     * @return a PoolStats instance
     */
    private PoolStats getPoolStats()
    {
        return new PoolStats(SECONDS.toMillis(1)) {
            @Override
            protected void update() {
                this.pendingThreads = HikariPool.this.getThreadsAwaitingConnection();
                this.idleConnections = HikariPool.this.getIdleConnections();
                this.totalConnections = HikariPool.this.getTotalConnections();
                this.activeConnections = HikariPool.this.getActiveConnections();
                this.maxConnections = config.getMaximumPoolSize();
                this.minConnections = config.getMinimumIdle();
            }
        };
    }

    // ***********************************************************************
    //                      Non-anonymous Inner-classes
    // ***********************************************************************

    /**
     * Creating and adding poolEntries (connections) to the pool.
     */
    private final class PoolEntryCreator implements Callable<Boolean>
    {
        private final String loggingPrefix;

        PoolEntryCreator(String loggingPrefix)
        {
            this.loggingPrefix = loggingPrefix;
        }

        @Override
        public Boolean call()
        {
            long sleepBackoff = 250L;
            while (poolState == POOL_NORMAL && shouldCreateAnotherConnection()) {
                final PoolEntry poolEntry = createPoolEntry();
                if (poolEntry != null) {
                    connectionBag.add(poolEntry);
                    LOGGER.debug("{} - Added connection {}", poolName, poolEntry.connection);
                    if (loggingPrefix != null) {
                        logPoolState(loggingPrefix);
                    }
                    return Boolean.TRUE;
                }

                // failed to get connection from db, sleep and retry
                quietlySleep(sleepBackoff);
                sleepBackoff = Math.min(SECONDS.toMillis(10), Math.min(connectionTimeout, (long) (sleepBackoff * 1.5)));
            }
            // Pool is suspended or shutdown or at max size
            return Boolean.FALSE;
        }

        /**
         * We only create connections if we need another idle connection or have threads still waiting
         * for a new connection.  Otherwise we bail out of the request to create.
         *
         * @return true if we should create a connection, false if the need has disappeared
         */
        private boolean shouldCreateAnotherConnection() {
            return getTotalConnections() < config.getMaximumPoolSize() &&
                    (connectionBag.getWaitingThreadCount() > 0 || getIdleConnections() < config.getMinimumIdle());
        }
    }

    /**
     * The house keeping task to retire and maintain minimum idle connections.
     */
    private final class HouseKeeper implements Runnable
    {
        private volatile long previous = plusMillis(currentTime(), -HOUSEKEEPING_PERIOD_MS);

        @Override
        public void run()
        {
            try {
                // refresh values in case they changed via MBean
                connectionTimeout = config.getConnectionTimeout();
                validationTimeout = config.getValidationTimeout();
                leakTaskFactory.updateLeakDetectionThreshold(config.getLeakDetectionThreshold());
                catalog = (config.getCatalog() != null && !config.getCatalog().equals(catalog)) ? config.getCatalog() : catalog;

                final long idleTimeout = config.getIdleTimeout();
                final long now = currentTime();

                // Detect retrograde time, allowing +128ms as per NTP spec.
                if (plusMillis(now, 128) < plusMillis(previous, HOUSEKEEPING_PERIOD_MS)) {
                    LOGGER.warn("{} - Retrograde clock change detected (housekeeper delta={}), soft-evicting connections from pool.",
                            poolName, elapsedDisplayString(previous, now));
                    previous = now;
                    softEvictConnections();
                    return;
                }
                else if (now > plusMillis(previous, (3 * HOUSEKEEPING_PERIOD_MS) / 2)) {
                    // No point evicting for forward clock motion, this merely accelerates connection retirement anyway
                    LOGGER.warn("{} - Thread starvation or clock leap detected (housekeeper delta={}).", poolName, elapsedDisplayString(previous, now));
                }

                previous = now;

                String afterPrefix = "Pool ";
                if (idleTimeout > 0L && config.getMinimumIdle() < config.getMaximumPoolSize()) {
                    logPoolState("Before cleanup ");
                    afterPrefix = "After cleanup  ";

                    final List<PoolEntry> notInUse = connectionBag.values(STATE_NOT_IN_USE);
                    int toRemove = notInUse.size() - config.getMinimumIdle();
                    for (PoolEntry entry : notInUse) {
                        if (toRemove > 0 && elapsedMillis(entry.lastAccessed, now) > idleTimeout && connectionBag.reserve(entry)) {
                            closeConnection(entry, "(connection has passed idleTimeout)");
                            toRemove--;
                        }
                    }
                }

                logPoolState(afterPrefix);

                fillPool(); // Try to maintain minimum connections
            }
            catch (Exception e) {
                LOGGER.error("Unexpected exception in housekeeping task", e);
            }
        }
    }
}
