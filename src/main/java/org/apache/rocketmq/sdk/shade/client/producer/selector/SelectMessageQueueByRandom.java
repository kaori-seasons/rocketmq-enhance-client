package org.apache.rocketmq.sdk.shade.client.producer.selector;

import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;
import java.util.Random;

public class SelectMessageQueueByRandom implements MessageQueueSelector {
    private Random random = new Random(System.currentTimeMillis());

    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        return mqs.get(this.random.nextInt(mqs.size()));
    }
}
