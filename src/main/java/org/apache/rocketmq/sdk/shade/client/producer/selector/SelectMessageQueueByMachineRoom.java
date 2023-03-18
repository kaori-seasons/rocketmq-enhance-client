package org.apache.rocketmq.sdk.shade.client.producer.selector;

import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;
import java.util.Set;

public class SelectMessageQueueByMachineRoom implements MessageQueueSelector {
    private Set<String> consumeridcs;

    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        return null;
    }

    public Set<String> getConsumeridcs() {
        return this.consumeridcs;
    }

    public void setConsumeridcs(Set<String> consumeridcs) {
        this.consumeridcs = consumeridcs;
    }
}
