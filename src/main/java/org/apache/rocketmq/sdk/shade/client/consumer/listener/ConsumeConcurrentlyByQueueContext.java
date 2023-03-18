package org.apache.rocketmq.sdk.shade.client.consumer.listener;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public class ConsumeConcurrentlyByQueueContext extends ConsumeConcurrentlyContext {
    private final MessageQueue messageQueue;

    public ConsumeConcurrentlyByQueueContext(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }
}
