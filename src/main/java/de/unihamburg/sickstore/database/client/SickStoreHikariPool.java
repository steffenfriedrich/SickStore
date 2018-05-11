package de.unihamburg.sickstore.database.client;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.database.ReadPreference;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.hikari.HikariConfig;
import de.unihamburg.sickstore.database.hikari.HikariPool;
import de.unihamburg.sickstore.database.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;

/**
 * Created by Steffen Friedrich on 16.08.2016.
 */
public class SickStoreHikariPool extends SickClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SickStoreHikariPool.class);

    private volatile HikariPool pool;
    private final AtomicBoolean isShutdown = new AtomicBoolean();

    SickConnection.ConnectionFactory connectionFactory;

    public SickStoreHikariPool(String host, int port, String destinationNode,
                               int maximumPoolSize, int minimumIdle) {
        super(host, port, destinationNode, new SystemTimeHandler());
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setMinimumIdle(minimumIdle);
        //hikariConfig.setConnectionTimeout(8000);
        //hikariConfig.setInitializationFailTimeout(100L);
        //hikariConfig.setIdleTimeout(30000);
        //hikariConfig.setLeakDetectionThreshold(60 * 1000);
        hikariConfig.validate();
        connectionFactory = new SickConnection.ConnectionFactory(this);
        this.pool = new HikariPool(this, hikariConfig);
    }

    public SickStoreHikariPool(String host, int port, String destinationNode, HikariConfig hikariConfig) {
        super(host, port, destinationNode, new SystemTimeHandler());
        hikariConfig.validate();
        this.connectionFactory = new SickConnection.ConnectionFactory(this);
        this.pool = new HikariPool(this, hikariConfig);
    }


    public Connection getConnection() throws SQLException {
        HikariPool result = pool;
        if (result == null) {
            synchronized (this) {
                result = pool;
                if (result == null) {
                    LOGGER.info("{} - Starting...", pool.getPoolName());
                    try {
                        pool = result = new HikariPool(this, new HikariConfig());
                    }
                    catch (HikariPool.PoolInitializationException pie) {
                        if (pie.getCause() instanceof SQLException) {
                            throw (SQLException) pie.getCause();
                        }
                        else {
                            throw pie;
                        }
                    }
                    LOGGER.info("{} - Start completed.", pool.getPoolName());
                }
            }
        }
        return result.getConnection();
    }

    synchronized public void disconnect() {
        if (isShutdown.getAndSet(true)) {
            return;
        }

       HikariPool p = pool;
        if (p != null) {
            try {
                LOGGER.info("{} - Shutdown initiated...", pool.getPoolName());
                p.shutdown();
                LOGGER.info("{} - Shutdown completed.", pool.getPoolName());
            }
            catch (InterruptedException e) {
                LOGGER.warn("{} - Interrupted during closing", pool.getPoolName(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public ServerResponse send(ClientRequest request) throws SQLException {
        return executeAsync(request).getUninterruptibly();
    }


    public ServerResponseFuture executeAsync(ClientRequest request) throws SQLException {
        ServerResponseFuture future = new ServerResponseFuture(this, request);
        new RequestHandler(this, future, request).send();
        return future;
    }

    public static void main(String[] args)  {

        String url =  "localhost";
        int port = 54000;

        int maxconnections = 8;
        int minimumidle = 2;

        // configure write concern
        WriteConcern writeConcern = new WriteConcern();
        writeConcern.setReplicaAcknowledgement(1);
        writeConcern.setJournaling(false);
        String destinationNode = "primary";
        ReadPreference readPreference = new ReadPreference(ReadPreference.PRIMARY);

        SickStoreHikariPool client = new SickStoreHikariPool(url, port, destinationNode, maxconnections, minimumidle);


        for (int i = 0; i < 10000; i++) {
            try {
                Version version = new Version();
                version.put("id", String.valueOf(i));
                client.insert("user", String.valueOf(i), version, writeConcern);
                quietlySleep(2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        client.disconnect();

        System.exit(0);
    }
}

