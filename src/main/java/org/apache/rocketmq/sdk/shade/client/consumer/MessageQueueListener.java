package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.Set;

public interface MessageQueueListener {
    void messageQueueChanged(String str, Set<MessageQueue> set, Set<MessageQueue> set2);
}
