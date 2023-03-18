package org.apache.rocketmq.sdk.shade.remoting;

import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface RemotingClient extends RemotingService {
    void updateNameServerAddressList(List<String> list);

    void updateProxyAddressList(List<String> list);

    List<String> getNameServerAddressList();

    RemotingCommand invokeSync(String str, RemotingCommand remotingCommand, long j) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(String str, RemotingCommand remotingCommand, long j, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(String str, RemotingCommand remotingCommand, long j) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void registerProcessor(int i, NettyRequestProcessor nettyRequestProcessor, ExecutorService executorService);

    void setCallbackExecutor(ExecutorService executorService);

    ExecutorService getCallbackExecutor();

    boolean isChannelWritable(String str);
}
