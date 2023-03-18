package org.apache.rocketmq.sdk.shade.client.consumer.listener;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public class ConsumeConcurrentlyByTopicContext extends ConsumeConcurrentlyContext {
    public ConsumeConcurrentlyByTopicContext() {
        this.ackIndex = 0;
    }

    @Override
    public MessageQueue getMessageQueue() {
        return null;
    }
}
