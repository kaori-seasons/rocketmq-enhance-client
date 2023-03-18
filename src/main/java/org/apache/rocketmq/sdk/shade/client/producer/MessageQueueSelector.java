package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;

public interface MessageQueueSelector {
    MessageQueue select(List<MessageQueue> list, Message message, Object obj);
}
