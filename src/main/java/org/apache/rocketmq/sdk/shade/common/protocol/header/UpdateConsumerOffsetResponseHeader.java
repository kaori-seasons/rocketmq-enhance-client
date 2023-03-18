package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class UpdateConsumerOffsetResponseHeader implements CommandCustomHeader {
    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
