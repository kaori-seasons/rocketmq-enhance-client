package org.apache.rocketmq.sdk.shade.client.consumer.listener;

public enum ConsumeOrderlyStatus {
    SUCCESS,
    ROLLBACK,
    COMMIT,
    SUSPEND_CURRENT_QUEUE_A_MOMENT
}
