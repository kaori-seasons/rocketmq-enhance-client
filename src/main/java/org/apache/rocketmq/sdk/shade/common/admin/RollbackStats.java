package org.apache.rocketmq.sdk.shade.common.admin;

public class RollbackStats {
    private String brokerName;
    private long queueId;
    private long brokerOffset;
    private long consumerOffset;
    private long timestampOffset;
    private long rollbackOffset;

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public long getQueueId() {
        return this.queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public long getBrokerOffset() {
        return this.brokerOffset;
    }

    public void setBrokerOffset(long brokerOffset) {
        this.brokerOffset = brokerOffset;
    }

    public long getConsumerOffset() {
        return this.consumerOffset;
    }

    public void setConsumerOffset(long consumerOffset) {
        this.consumerOffset = consumerOffset;
    }

    public long getTimestampOffset() {
        return this.timestampOffset;
    }

    public void setTimestampOffset(long timestampOffset) {
        this.timestampOffset = timestampOffset;
    }

    public long getRollbackOffset() {
        return this.rollbackOffset;
    }

    public void setRollbackOffset(long rollbackOffset) {
        this.rollbackOffset = rollbackOffset;
    }
}
