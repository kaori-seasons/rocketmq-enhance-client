package org.apache.rocketmq.sdk.shade.remoting;

import org.apache.rocketmq.sdk.shade.remoting.common.Pair;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

public interface RemotingServer extends RemotingService {
    void registerProcessor(int i, NettyRequestProcessor nettyRequestProcessor, ExecutorService executorService);

    void registerDefaultProcessor(NettyRequestProcessor nettyRequestProcessor, ExecutorService executorService);

    int localListenPort();

    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int i);

    RemotingCommand invokeSync(Channel channel, RemotingCommand remotingCommand, long j) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(Channel channel, RemotingCommand remotingCommand, long j, InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(Channel channel, RemotingCommand remotingCommand, long j) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;
}
