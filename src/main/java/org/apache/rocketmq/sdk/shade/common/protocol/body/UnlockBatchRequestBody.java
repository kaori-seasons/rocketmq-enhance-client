package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;
import java.util.Set;

public class UnlockBatchRequestBody extends RemotingSerializable {
    private String consumerGroup;
    private String clientId;
    private Set<MessageQueue> mqSet = new HashSet();

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

    public Set<MessageQueue> getMqSet() {
        return this.mqSet;
    }

    public void setMqSet(Set<MessageQueue> mqSet) {
        this.mqSet = mqSet;
    }
}
