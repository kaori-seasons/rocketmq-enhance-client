package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class UnregisterClientRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String clientID;
    @CFNullable
    private String producerGroup;
    @CFNullable
    private String consumerGroup;

    public String getClientID() {
        return this.clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getProducerGroup() {
        return this.producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
