package org.apache.rocketmq.sdk.shade.client.consumer.listener;

public enum ConsumeExactlyOnceStatus {
    NO_EXACTLYONCE,
    EXACTLYONCE_PASS,
    EXACTLYONCE_DEDUP
}
