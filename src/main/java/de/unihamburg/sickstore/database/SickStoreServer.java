package de.unihamburg.sickstore.database;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.clientdelay.ZeroClientDelay;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.config.InstanceFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Friedrich on 11.08.2016.
 */
public class SickStoreServer {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final InetSocketAddress address;

    /**
     * Creates a new instance from a given config object.
     *
     * @param config
     */
    @SuppressWarnings("unused")
    public static SickStoreServer newInstanceFromConfig(Map<String, Object> config) {
        int port = (int) config.getOrDefault("port", 54000);
        QueryHandlerInterface queryHandler = (QueryHandlerInterface) InstanceFactory.newInstanceFromConfig(
                (Map<String, Object>) config.get("queryHandler")
        );

        return new SickStoreServer(port, queryHandler);
    }

    public SickStoreServer(int port, final QueryHandlerInterface queryHandler) {
        address = new InetSocketAddress(port);
        bootstrap.group(group).channel(NioServerSocketChannel.class)
                .childHandler(new SickStoreServerInitializer(queryHandler));

    }

    public void start() {
        Runnable r = new ServerThread(this);
        new Thread(r).start();
    }

    public void connect() throws InterruptedException {
        try {
            ChannelFuture future = bootstrap.bind(address);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        System.out.println("SickStore is waiting for connection on port " + address.getPort());
                    } else {
                        System.err.println("Failed to start SickStore =======>");
                        channelFuture.cause().printStackTrace();
                    }
                }
            });
            channel = future.channel();
            channel.closeFuture().syncUninterruptibly();
        } finally {
            group.shutdownGracefully().sync();
        }
    }


    public void shutdown() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Please give port as argument");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);


        TimeHandler timeHandler = new FakeTimeHandler();
        BasicAnomalyGenerator anomalyGenerator = new BasicAnomalyGenerator(
                new ConstantStaleness(500, 0),
                new ZeroClientDelay()
        );
        Store store = new Store(timeHandler);
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("node1"));
        nodes.add(new Node("node2"));
        nodes.add(new Node("node3"));
        QueryHandlerInterface queryHandler = new QueryHandler(store, anomalyGenerator, nodes, timeHandler, 0, false, false);


        SickStoreServer endpoint = new SickStoreServer(port, queryHandler);
        endpoint.start();
    }

    class ServerThread implements Runnable {
        SickStoreServer server;

        public ServerThread(SickStoreServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                server.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
