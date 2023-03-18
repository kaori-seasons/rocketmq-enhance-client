package org.apache.rocketmq.sdk.api.exactlyonce.manager.database;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public class LoadRecordDo {
    private MessageQueue messageQueue;
    private String consumerGroup;
    private Long offset;
    private Long timestamp;
    private int count;

    public LoadRecordDo(MessageQueue messageQueue, String consumerGroup, Long offset, Long timestamp, int count) {
        this.messageQueue = messageQueue;
        this.consumerGroup = consumerGroup;
        this.offset = offset;
        this.timestamp = timestamp;
        this.count = count;
    }

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Long getOffset() {
        return this.offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String toString() {
        return "LoadRecordDo{messageQueue=" + this.messageQueue + ", consumerGroup='" + this.consumerGroup + "', offset=" + this.offset + ", timestamp=" + this.timestamp + ", count=" + this.count + '}';
    }
}
