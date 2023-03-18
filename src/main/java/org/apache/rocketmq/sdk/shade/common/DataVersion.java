package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.concurrent.atomic.AtomicLong;

public class DataVersion extends RemotingSerializable {
    private long timestamp = System.currentTimeMillis();
    private AtomicLong counter = new AtomicLong(0);

    public void assignNewOne(DataVersion dataVersion) {
        this.timestamp = dataVersion.timestamp;
        this.counter.set(dataVersion.counter.get());
    }

    public void nextVersion() {
        this.timestamp = System.currentTimeMillis();
        this.counter.incrementAndGet();
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public AtomicLong getCounter() {
        return this.counter;
    }

    public void setCounter(AtomicLong counter) {
        this.counter = counter;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataVersion that = (DataVersion) o;
        if (this.timestamp != that.timestamp) {
            return false;
        }
        return (this.counter == null || that.counter == null) ? null == this.counter && null == that.counter : this.counter.longValue() == that.counter.longValue();
    }

    public int hashCode() {
        int result = (int) (this.timestamp ^ (this.timestamp >>> 32));
        if (null != this.counter) {
            long l = this.counter.get();
            result = (31 * result) + ((int) (l ^ (l >>> 32)));
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DataVersion[");
        sb.append("timestamp=").append(this.timestamp);
        sb.append(", counter=").append(this.counter);
        sb.append(']');
        return sb.toString();
    }
}
