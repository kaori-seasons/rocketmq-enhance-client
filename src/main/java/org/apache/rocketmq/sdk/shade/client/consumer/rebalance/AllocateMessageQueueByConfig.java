package org.apache.rocketmq.sdk.shade.client.consumer.rebalance;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;

public class AllocateMessageQueueByConfig implements AllocateMessageQueueStrategy {
    private List<MessageQueue> messageQueueList;

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        return this.messageQueueList;
    }

    @Override
    public String getName() {
        return "CONFIG";
    }

    public List<MessageQueue> getMessageQueueList() {
        return this.messageQueueList;
    }

    public void setMessageQueueList(List<MessageQueue> messageQueueList) {
        this.messageQueueList = messageQueueList;
    }
}
