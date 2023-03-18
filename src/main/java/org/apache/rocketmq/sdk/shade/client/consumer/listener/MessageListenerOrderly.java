package org.apache.rocketmq.sdk.shade.client.consumer.listener;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.List;

public interface MessageListenerOrderly extends MessageListener {
    ConsumeOrderlyStatus consumeMessage(List<MessageExt> list, ConsumeOrderlyContext consumeOrderlyContext);
}
