package org.apache.rocketmq.sdk.shade.client.consumer.listener;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.List;

public interface MessageListenerConcurrently extends MessageListener {
    ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext);
}
