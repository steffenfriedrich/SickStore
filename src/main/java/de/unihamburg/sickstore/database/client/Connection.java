package de.unihamburg.sickstore.database.client;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.util.concurrent.*;
import de.unihamburg.sickstore.kryo.KryoMessageRegistrar;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.kryo.KryoDecoder;
import de.unihamburg.sickstore.kryo.KryoEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.*;


import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Steffen Friedrich on 16.08.2016.
 */
public class Connection {
    private final String name;
    private final InetSocketAddress address;
    private final ConnectionFactory connectionFactory;
    private volatile Channel channel;
    private final Dispatcher dispatcher;
    private final ConnectionPool pool;

    private final AtomicInteger inFlight;
    private final AtomicReference<ConnectionCloseFuture> closeFuture = new AtomicReference<ConnectionCloseFuture>();

    public Connection(ConnectionPool pool, String name, InetSocketAddress address, ConnectionFactory connectionFactory) {
        this.pool = pool;
        this.name = name;
        this.address = address;
        this.connectionFactory = connectionFactory;
        this.dispatcher = new Dispatcher(this);
        this.inFlight = new AtomicInteger(0);
    }

    ListenableFuture<Void> initAsync() {
        final SettableFuture<Void> channelReadyFuture = SettableFuture.create();
        try {
            Bootstrap bootstrap = connectionFactory.newBootstrap();
            bootstrap.handler(new Initializer(this));
            ChannelFuture future = bootstrap.connect(address);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    channel =future.channel();
                    Connection.this.connectionFactory.allChannels.add(channel);
                    if (!future.isSuccess()) {
                        System.out.println(String.format("Error connecting to %s%s", Connection.this.address, extractMessage(future.cause())));
                    } else {
                        channel.closeFuture().addListener(future1 -> Connection.this.close());
                        channelReadyFuture.set(null);
                    }
                }
            });
        } catch (RuntimeException e) {
            //closeAsync().force();
            throw e;
        }

        Executor initExecutor = MoreExecutors.directExecutor();

        ListenableFuture<Void> initFuture = Futures.withFallback(channelReadyFuture, new FutureFallback<Void>() {
            @Override
            public ListenableFuture<Void> create(Throwable t) throws Exception {
                SettableFuture<Void> f = SettableFuture.create();
                close().force();
                f.setException(t);
                return f;
            }
        }, initExecutor);
        Futures.addCallback(initFuture, new FailureCallback<Void>(){
            @Override
            public void onFailure(Throwable t) {
                if (!isClosed()) {
                    close().force();
                }
            }
        });

        return initFuture;
    }

    boolean isClosed() {
        return closeFuture.get() != null;
    }

    CloseFuture close() {
        ConnectionCloseFuture future = new ConnectionCloseFuture();
        if (!closeFuture.compareAndSet(null, future)) {
            // close had already been called, return the existing future
            return closeFuture.get();
        }
        assert isClosed();
        boolean terminated = tryTerminate(false);
        return future;
    }

    boolean tryTerminate(boolean force) {
        assert isClosed();
        ConnectionCloseFuture future = closeFuture.get();

        if (future.isDone()) {
            return true;
        } else {
            if (force || dispatcher.getPending().isEmpty()) {
                if (force)
                future.force();
                return true;
            } else {
                return false;
            }
        }
    }

    ResponseHandler write(ResponseCallback callback) {
        ResponseHandler h = new ResponseHandler(this, callback);
        dispatcher.add(h);
        ClientRequest request = callback.request().setStreamId(h.streamId);
        channel.writeAndFlush(request);
        return h;
    }

    private static String extractMessage(Throwable t) {
        if (t == null)
            return "";
        String msg = t.getMessage() == null || t.getMessage().isEmpty()
                ? t.toString()
                : t.getMessage();
        return " (" + msg + ')';
    }

    void release() {
        inFlight.decrementAndGet();
    }

    public String getName() {
        return name;
    }

    public AtomicInteger getInFlight() {
        return inFlight;
    }

    static class ConnectionFactory {
        final SickStoreClient client;
        final EventLoopGroup eventLoopGroup;

        private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        private final AtomicInteger idGenerator = new AtomicInteger(1);
        private volatile boolean isShutdown;

        ConnectionFactory(SickStoreClient client) {
            this.client = client;
            this.eventLoopGroup = new NioEventLoopGroup(0, threadFactory("nio-worker"));
        }

        /**
         * Opens a new connection
         *
         * @return the newly created (and initialized) connection.
         * @throws InterruptedException
         * @throws ConnectException
         */
        Connection open(ConnectionPool pool, String host) throws InterruptedException, ConnectException, ExecutionException {
            InetSocketAddress address = new InetSocketAddress(host, getPort());
            Connection connection = new Connection(pool, buildConnectionName(address), address, this);
            connection.initAsync().get();
            return connection;
        }


        void shutdown() {
            // Make sure we skip creating connection from now on.
            isShutdown = true;
            // All channels should be closed already, we call this just to be sure. And we know
            // we're not on an I/O thread or anything, so just call await.
            allChannels.close().awaitUninterruptibly();
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }

        private Bootstrap newBootstrap() {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup).channel(NioSocketChannel.class);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            return b;
        }

        int getPort() {
            return client.getPort();
        }

        private String buildConnectionName(InetSocketAddress address) {
            return address.toString() + '-' + idGenerator.getAndIncrement();
        }

        ThreadFactory threadFactory(String name) {
            return new ThreadFactoryBuilder().setNameFormat("SickStore-" + name + "-%d").build();
        }
    }

    /**
     * Created by Steffen Friedrich on 16.08.2016.
     */
    private static class Initializer extends ChannelInitializer<SocketChannel> {

        private final Connection connection;
        private final IdleStateHandler idleStateHandler;

        Initializer(Connection connection) {
            this.connection = connection;
            this.idleStateHandler = new IdleStateHandler(0, 0, 30);
        }

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();

            Kryo kryo = new Kryo();
            KryoMessageRegistrar.register(kryo);
            KryoEncoder ke = new KryoEncoder(kryo);
            KryoDecoder kd = new KryoDecoder(kryo);

            pipeline.addLast("messageEncoder", ke);
            pipeline.addLast("messageDecoder", kd);
            pipeline.addLast("idleStateHandler", idleStateHandler);
            pipeline.addLast("sickStoreClientHandler", connection.dispatcher);
        }
    }

    interface ResponseCallback {
        ClientRequest request();
        void onSet(Connection connection, ServerResponse response);
    }

    static class ResponseHandler {
        final int streamId;
        final ResponseCallback callback;
        final Connection connection;

        ResponseHandler(Connection connection, ResponseCallback callback) {
            this.connection = connection;
            this.streamId = connection.dispatcher.idGenerator.next();
            this.callback = callback;
        }

        boolean cancelHandler() {
            connection.dispatcher.removeHandler(this, false);
            return true;
        }
    }



    public static abstract class FailureCallback<V> implements FutureCallback<V> {
        @Override
        public void onSuccess(V result) { /* nothing */ }
    }

    private class ConnectionCloseFuture extends CloseFuture {

        @Override
        public ConnectionCloseFuture force() {
            // Note: we must not call releaseExternalResources on the bootstrap, because this shutdown the executors, which are shared

            // This method can be thrown during initialization, at which point channel is not yet set. This is ok.
            if (channel == null) {
                set(null);
                return this;
            }

            ChannelFuture future = channel.close();
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    connectionFactory.allChannels.remove(channel);
                    if (future.cause() != null) {
                        ConnectionCloseFuture.this.setException(future.cause());
                    } else
                        ConnectionCloseFuture.this.set(null);
                }
            });
            return this;
        }
    }
}
