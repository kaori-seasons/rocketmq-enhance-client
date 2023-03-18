package org.apache.rocketmq.sdk.shade.remoting;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

public interface RPCHook {
    void doBeforeRequest(String str, RemotingCommand remotingCommand);

    void doAfterResponse(String str, RemotingCommand remotingCommand, RemotingCommand remotingCommand2);
}
