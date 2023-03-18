package org.apache.rocketmq.sdk.shade.client.consumer.rebalance;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.ArrayList;
import java.util.List;

public class AllocateMessageQueueAveragely implements AllocateMessageQueueStrategy {
    private final InternalLogger log = ClientLogger.getLog();

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        int averageSize;
        if (currentCID == null || currentCID.length() < 1) {
            throw new IllegalArgumentException("currentCID is empty");
        } else if (mqAll == null || mqAll.isEmpty()) {
            throw new IllegalArgumentException("mqAll is null or mqAll empty");
        } else if (cidAll == null || cidAll.isEmpty()) {
            throw new IllegalArgumentException("cidAll is null or cidAll empty");
        } else {
            List<MessageQueue> result = new ArrayList<>();
            if (!cidAll.contains(currentCID)) {
                this.log.info("[BUG] ConsumerGroup: {} The consumerId: {} not in cidAll: {}", consumerGroup, currentCID, cidAll);
                return result;
            }
            int index = cidAll.indexOf(currentCID);
            int mod = mqAll.size() % cidAll.size();
            if (mqAll.size() <= cidAll.size()) {
                averageSize = 1;
            } else {
                averageSize = (mod <= 0 || index >= mod) ? mqAll.size() / cidAll.size() : (mqAll.size() / cidAll.size()) + 1;
            }
            int startIndex = (mod <= 0 || index >= mod) ? (index * averageSize) + mod : index * averageSize;
            int range = Math.min(averageSize, mqAll.size() - startIndex);
            for (int i = 0; i < range; i++) {
                result.add(mqAll.get((startIndex + i) % mqAll.size()));
            }
            return result;
        }
    }

    @Override
    public String getName() {
        return "AVG";
    }
}
