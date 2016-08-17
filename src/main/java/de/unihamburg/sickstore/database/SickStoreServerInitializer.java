package de.unihamburg.sickstore.database;

import com.esotericsoftware.kryo.Kryo;
import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.kryo.KryoMessageRegistrar;
import de.unihamburg.sickstore.kryo.KryoDecoder;
import de.unihamburg.sickstore.kryo.KryoEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/**
 * Created by Friedrich on 11.08.2016.
 */
public class SickStoreServerInitializer extends ChannelInitializer<Channel> {
    private final QueryHandlerInterface handler;

    public SickStoreServerInitializer(QueryHandlerInterface handler) {
        this.handler = handler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        Kryo kryo = new Kryo();
        KryoMessageRegistrar.register(kryo);
        KryoEncoder ke = new KryoEncoder(kryo);
        KryoDecoder kd = new KryoDecoder(kryo);

        pipeline.addLast(ke);
        pipeline.addLast(kd);
        pipeline.addLast(new SickStoreServerHandler(handler));
    }
}
