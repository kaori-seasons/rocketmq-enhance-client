package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class GetConsumerRunningInfoRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String consumerGroup;
    @CFNotNull
    private String clientId;
    @CFNullable
    private boolean jstackEnable;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isJstackEnable() {
        return this.jstackEnable;
    }

    public void setJstackEnable(boolean jstackEnable) {
        this.jstackEnable = jstackEnable;
    }
}
