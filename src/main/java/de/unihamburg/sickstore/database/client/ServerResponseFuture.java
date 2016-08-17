package de.unihamburg.sickstore.database.client;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

import java.util.concurrent.ExecutionException;

/**
 * Created by Steffen Friedrich on 17.08.2016.
 */
class ServerResponseFuture   extends AbstractFuture<ServerResponse> implements  RequestHandler.Callback, ListenableFuture<ServerResponse> {

    private final SickStoreClient client;
    private final ClientRequest request;
    private RequestHandler handler;

    ServerResponseFuture(SickStoreClient client, ClientRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public void onSet(Connection connection, ServerResponse response, ClientRequest request) {
        set(response);
    }

    @Override
    public void onSet(Connection connection, ServerResponse response) {
        onSet(connection, response, null);
    }

    public ServerResponse getUninterruptibly() throws ExecutionException {
        return Uninterruptibles.getUninterruptibly(this);
    }

    public void register(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public ClientRequest request() {
        return request;
    }

}
