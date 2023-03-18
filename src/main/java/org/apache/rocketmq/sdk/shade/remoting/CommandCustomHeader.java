package org.apache.rocketmq.sdk.shade.remoting;

import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public interface CommandCustomHeader {
    void checkFields() throws RemotingCommandException;
}
