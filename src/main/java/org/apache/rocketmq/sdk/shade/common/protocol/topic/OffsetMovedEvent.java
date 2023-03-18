package org.apache.rocketmq.sdk.shade.common.protocol.topic;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import org.springframework.beans.PropertyAccessor;

public class OffsetMovedEvent extends RemotingSerializable {
    private String consumerGroup;
    private MessageQueue messageQueue;
    private long offsetRequest;
    private long offsetNew;

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public long getOffsetRequest() {
        return this.offsetRequest;
    }

    public void setOffsetRequest(long offsetRequest) {
        this.offsetRequest = offsetRequest;
    }

    public long getOffsetNew() {
        return this.offsetNew;
    }

    public void setOffsetNew(long offsetNew) {
        this.offsetNew = offsetNew;
    }

    public String toString() {
        return "OffsetMovedEvent [consumerGroup=" + this.consumerGroup + ", messageQueue=" + this.messageQueue + ", offsetRequest=" + this.offsetRequest + ", offsetNew=" + this.offsetNew + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
