package de.unihamburg.sickstore.database.client;

import com.google.common.collect.Sets;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Friedrich on 17.08.2016.
 */
public class RequestHandler {
    final String id;
    private final Client client;
    private final Callback callback;
    private final ClientRequest request;
    private final AtomicInteger executionCount = new AtomicInteger();
    private final Set<Execution> runningExecutions = Sets.newCopyOnWriteArraySet();

    public RequestHandler(Client client, Callback callback, ClientRequest request) {
        this.id = Long.toString(System.identityHashCode(this));
        this.client = client;
        this.callback = callback;
        this.request = request;

        this.callback.register(this);
    }

    public void send() throws SQLException {
        ClientRequest request = callback.request();
        int position = executionCount.incrementAndGet();
        Execution execution = new Execution(request, position);
        runningExecutions.add(execution);
        execution.sendRequest();
    }

    private void setFinalResult(Execution execution, Connection connection, ServerResponse response) {
        callback.onSet(connection, response, request);
    }

    interface Callback extends Connection.ResponseCallback {
        void onSet(Connection connection, ServerResponse response, ClientRequest request);

        void register(RequestHandler handler);
    }

    class Execution implements Connection.ResponseCallback {
        final String id;
        private final ClientRequest request;

        Execution(ClientRequest request, int position) {
            this.id = RequestHandler.this.id + "-" + position;
            this.request = request;
        }

        void sendRequest() throws SQLException {
            Connection connection = client.getConnection();
            connection.write(this);
        }

        @Override
        public ClientRequest request() {
            return request;
        }

        @Override
        public void onSet(Connection connection, ServerResponse response) {
            connection.release();
            RequestHandler.this.setFinalResult(this, connection, response);
        }

    }
}
