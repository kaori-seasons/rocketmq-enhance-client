package org.apache.rocketmq.sdk.shade.common.stats;

class CallSnapshot {
    private final long timestamp;
    private final long times;
    private final long value;

    public CallSnapshot(long timestamp, long times, long value) {
        this.timestamp = timestamp;
        this.times = times;
        this.value = value;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getTimes() {
        return this.times;
    }

    public long getValue() {
        return this.value;
    }
}
