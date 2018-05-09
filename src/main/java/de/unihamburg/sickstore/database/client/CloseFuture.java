package de.unihamburg.sickstore.database.client;

import com.google.common.util.concurrent.AbstractFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;


public class CloseFuture extends AbstractFuture<Void> {
    private final SickConnection connection;

    CloseFuture(SickConnection connection) {
        this.connection = connection;
    }

    public CloseFuture force() {
        // Note: we must not call releaseExternalResources on the bootstrap, because this shutdown the executors, which are shared

        // This method can be thrown during initialization, at which point channel is not yet set. This is ok.
        if (connection.getChannel() == null) {
            set(null);
            return this;
        }

        Channel channel = connection.getChannel();
        SickConnection.ConnectionFactory connectionFactory = connection.getConnectionFactory();

        ChannelFuture future = channel.close();
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                connectionFactory.getAllChannels().remove(channel);
                if (future.cause() != null) {
                    CloseFuture.this.setException(future.cause());
                } else
                    CloseFuture.this.set(null);
            }
        });
        return this;
    }
}