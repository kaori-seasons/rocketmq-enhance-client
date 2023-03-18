package org.apache.rocketmq.sdk.shade.common;

public class SystemClock {
    public long now() {
        return System.currentTimeMillis();
    }
}
