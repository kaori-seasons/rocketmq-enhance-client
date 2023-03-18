package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class QueryConsumeQueueRequestHeader implements CommandCustomHeader {
    private String topic;
    private int queueId;
    private long index;
    private int count;
    private String consumerGroup;

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQueueId() {
        return this.queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getIndex() {
        return this.index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
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
