package de.unihamburg.sickstore.database.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Steffen Friedrich on 17.08.2016.
 */
public class ConnectionPool {
    private final Runnable newConnectionTask;

    private final SickStoreClient client;
    private final int maxConnections;
    final List<Connection> connections;
    private final AtomicInteger open;

    public ConnectionPool(SickStoreClient client) {
        this.client = client;
        this.open = new AtomicInteger();
        this.maxConnections = client.getMaxConnections();
        this.connections = new CopyOnWriteArrayList<Connection>();
        this.newConnectionTask = new Runnable() {
            @Override
            public void run() {
               while(addConnectionIfUnderMaximum()) {

               }
                System.out.println(String.format("Connection established with SickStore %s:%s, initializing transport", client.getHost(), client.getPort() ));
            }
        };
        client.blockingExecutor().submit(newConnectionTask);
    }

    private boolean addConnectionIfUnderMaximum() {
        for (; ; ) {
            int opened = open.get();
            if (opened >= maxConnections)
                return false;

            if (open.compareAndSet(opened, opened + 1))
                break;
        }
        Connection newConnection = null;
        try {
            newConnection = client.getConnectionFactory().open(client.getHost());
            connections.add(newConnection);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Connection borrowConnection() throws SQLException {
        int minInFlight = Integer.MAX_VALUE;
        Connection leastBusy = null;
        for (Connection connection : connections) {
            int inFlight = connection.inFlight.get();
            if (inFlight < minInFlight) {
                minInFlight = inFlight;
                leastBusy = connection;
            }
        }
        while (true) {
            int inFlight = leastBusy.inFlight.get();
            if (leastBusy.inFlight.compareAndSet(inFlight, inFlight + 1)) {
                break;
            }
        }

        return leastBusy;
    }

    private void close(final Connection connection) {
        connection.closeAsync();
    }

    public void close() {
        connections.forEach(connection -> connection.closeAsync());
    }

}
