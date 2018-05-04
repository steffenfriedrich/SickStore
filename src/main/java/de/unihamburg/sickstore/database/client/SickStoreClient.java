package de.unihamburg.sickstore.database.client;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.ReadPreference;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Steffen Friedrich on 16.08.2016.
 */
public class SickStoreClient extends SickClient implements Client {
    private static final Logger logger = LoggerFactory.getLogger(SickStoreClient.class);

    private ConnectionPool connectionPool;
    private final int maxConnections;

    Connection.ConnectionFactory connectionFactory;

    ListeningExecutorService blockingExecutor;
    LinkedBlockingQueue<Runnable> blockingExecutorQueue;

    public SickStoreClient(String host, int port, String destinationNode) throws  ConnectException {
        this(host, port, destinationNode, new SystemTimeHandler(), 84);
    }

    public SickStoreClient(String host, int port, String destinationNode, TimeHandler timeHandler) throws  ConnectException {
        this(host, port, destinationNode, timeHandler, 84);
    }

    public SickStoreClient(String host, int port, String destinationNode, TimeHandler timeHandler, int maxConnections) throws  ConnectException {
        super(host, port, destinationNode, new SystemTimeHandler());
        this.maxConnections = maxConnections;

        this.blockingExecutorQueue = new LinkedBlockingQueue<Runnable>();
        this.blockingExecutor = makeExecutor(6, "blocking-task-worker", blockingExecutorQueue);

        this.connectionFactory = new Connection.ConnectionFactory(this);

        connectionPool = new ConnectionPool(this);


        long startTime = System.currentTimeMillis();
        while(connectionPool.connections.size() < maxConnections) {
            if((System.currentTimeMillis()-startTime)>5000) {
                throw new ConnectException("SickStore connection timeout ...");
            }
        }
    }


    synchronized public void disconnect() {
        this.connectionPool.close();
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.borrowConnection();
    }

    ListeningExecutorService blockingExecutor() {
        return blockingExecutor;
    }

    public ServerResponse send(ClientRequest request) throws SQLException {
        return executeAsync(request).getUninterruptibly();
    }


    public ServerResponseFuture executeAsync(ClientRequest request) throws SQLException {
        ServerResponseFuture future = new ServerResponseFuture(this, request);
        new RequestHandler(this, future, request).send();
        return future;
    }

    ThreadFactory threadFactory(String name) {
        return new ThreadFactoryBuilder().setNameFormat("SickStore-" + name + "-%d").build();
    }

    private ListeningExecutorService makeExecutor(int threads, String name, LinkedBlockingQueue<Runnable> workQueue) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads,
                threads,
                30,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory(name));

        executor.allowCoreThreadTimeOut(true);
        return MoreExecutors.listeningDecorator(executor);
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}

