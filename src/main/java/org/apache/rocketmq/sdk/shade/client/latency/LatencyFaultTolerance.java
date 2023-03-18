package org.apache.rocketmq.sdk.shade.client.latency;

public interface LatencyFaultTolerance<T> {
    void updateFaultItem(T t, long j, long j2);

    boolean isAvailable(T t);

    void remove(T t);

    T pickOneAtLeast();
}
