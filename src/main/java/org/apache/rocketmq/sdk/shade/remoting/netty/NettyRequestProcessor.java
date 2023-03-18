package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestProcessor {
    RemotingCommand processRequest(ChannelHandlerContext channelHandlerContext, RemotingCommand remotingCommand) throws Exception;

    boolean rejectRequest();
}
