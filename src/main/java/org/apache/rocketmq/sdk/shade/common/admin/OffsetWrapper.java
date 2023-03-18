package org.apache.rocketmq.sdk.shade.common.admin;

public class OffsetWrapper {
    private long brokerOffset;
    private long consumerOffset;
    private long lastTimestamp;

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

    public long getLastTimestamp() {
        return this.lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
