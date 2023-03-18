package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class GetConsumeStatsInBrokerHeader implements CommandCustomHeader {
    @CFNotNull
    private boolean isOrder;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public boolean isOrder() {
        return this.isOrder;
    }

    public void setIsOrder(boolean isOrder) {
        this.isOrder = isOrder;
    }
}
