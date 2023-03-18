package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;

public class ConsumeByWho extends RemotingSerializable {
    private HashSet<String> consumedGroup = new HashSet<>();
    private HashSet<String> notConsumedGroup = new HashSet<>();
    private String topic;
    private int queueId;
    private long offset;

    public HashSet<String> getConsumedGroup() {
        return this.consumedGroup;
    }

    public void setConsumedGroup(HashSet<String> consumedGroup) {
        this.consumedGroup = consumedGroup;
    }

    public HashSet<String> getNotConsumedGroup() {
        return this.notConsumedGroup;
    }

    public void setNotConsumedGroup(HashSet<String> notConsumedGroup) {
        this.notConsumedGroup = notConsumedGroup;
    }

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

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
