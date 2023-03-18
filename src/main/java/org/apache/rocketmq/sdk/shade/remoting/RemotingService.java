package org.apache.rocketmq.sdk.shade.remoting;

public interface RemotingService {
    void start();

    void shutdown();

    void registerRPCHook(RPCHook rPCHook);
}
