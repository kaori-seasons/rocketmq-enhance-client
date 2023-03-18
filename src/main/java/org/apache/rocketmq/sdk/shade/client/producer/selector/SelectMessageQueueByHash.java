package org.apache.rocketmq.sdk.shade.client.producer.selector;

import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;

public class SelectMessageQueueByHash implements MessageQueueSelector {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        int value = arg.hashCode();
        if (value < 0) {
            value = Math.abs(value);
        }
        return mqs.get(value % mqs.size());
    }
}
