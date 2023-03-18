package org.apache.rocketmq.sdk.shade.client.consumer;

public enum PullStatus {
    FOUND,
    NO_NEW_MSG,
    NO_MATCHED_MSG,
    OFFSET_ILLEGAL
}
