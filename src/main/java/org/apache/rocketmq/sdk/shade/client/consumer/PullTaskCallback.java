package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public interface PullTaskCallback {
    void doPullTask(MessageQueue messageQueue, PullTaskContext pullTaskContext);
}
