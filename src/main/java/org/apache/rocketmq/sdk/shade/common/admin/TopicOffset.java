package org.apache.rocketmq.sdk.shade.common.admin;

public class TopicOffset {
    private long minOffset;
    private long maxOffset;
    private long lastUpdateTimestamp;

    public long getMinOffset() {
        return this.minOffset;
    }

    public void setMinOffset(long minOffset) {
        this.minOffset = minOffset;
    }

    public long getMaxOffset() {
        return this.maxOffset;
    }

    public void setMaxOffset(long maxOffset) {
        this.maxOffset = maxOffset;
    }

    public long getLastUpdateTimestamp() {
        return this.lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }
}
