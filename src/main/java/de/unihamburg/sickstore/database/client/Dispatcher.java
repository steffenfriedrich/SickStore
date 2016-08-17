package de.unihamburg.sickstore.database.client;

import de.unihamburg.sickstore.database.messages.ServerResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Steffen Friedrich on 16.08.2016.
 */
class Dispatcher extends SimpleChannelInboundHandler<Object> {

    final IDGenerator idGenerator;

    private final ConcurrentMap<Integer, Connection.ResponseHandler> pending = new ConcurrentHashMap<Integer, Connection.ResponseHandler>();
    private final Connection connection;

    Dispatcher(Connection connection) {
        idGenerator = IDGenerator.newInstance();
        this.connection = connection;
    }

    void add(Connection.ResponseHandler handler) {
        Connection.ResponseHandler old = pending.put(handler.streamId, handler);
        assert old == null;
    }

    void removeHandler(Connection.ResponseHandler handler, boolean releaseStreamId) {
        if (!releaseStreamId) {
            idGenerator.mark(handler.streamId);
        }
        boolean removed = pending.remove(handler.streamId, handler);
        if (!removed) {
            // We raced, so if we marked the streamId above, that was wrong.
            if (!releaseStreamId)
                idGenerator.unmark(handler.streamId);
            return;
        }

        if (releaseStreamId) {
            idGenerator.release(handler.streamId);
        }
    }

    public ConcurrentMap<Integer, Connection.ResponseHandler> getPending() {
        return pending;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object object) throws Exception {
        if (object instanceof ServerResponse) {
            ServerResponse response = (ServerResponse) object;
            int streamId = response.getStreamId();
            Connection.ResponseHandler handler = pending.remove(streamId);
            idGenerator.release(streamId);
            handler.callback.onSet(connection, response);

        }
    }
}