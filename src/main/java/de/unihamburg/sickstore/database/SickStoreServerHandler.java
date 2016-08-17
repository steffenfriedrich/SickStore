package de.unihamburg.sickstore.database;

import de.unihamburg.sickstore.backend.QueryHandlerInterface;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import de.unihamburg.sickstore.database.messages.ServerResponseCleanup;
import de.unihamburg.sickstore.database.messages.ServerResponseException;
import de.unihamburg.sickstore.database.messages.exception.UnknownMessageTypeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


/**
 * Created by Steffen Friedrich on 11.08.2016.
 */
public class SickStoreServerHandler extends SimpleChannelInboundHandler<Object> {

    final private QueryHandlerInterface queryHandler;

    public SickStoreServerHandler(QueryHandlerInterface queryHandler) {
        this.queryHandler = queryHandler;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected on port" + ctx.channel().remoteAddress());
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        ServerResponse response = null;
        if (msg instanceof ClientRequest) {
            response = queryHandler.processQuery((ClientRequest) msg);
        } else {
            response = new ServerResponseException(
                    -1,
                    new UnknownMessageTypeException(
                            "Cannot process request; unknown message type: " + msg.getClass()
                    )
            );
        }
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
