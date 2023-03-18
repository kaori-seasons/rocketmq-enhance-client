package org.apache.rocketmq.sdk.api.exactlyonce.manager.util;

import org.apache.rocketmq.sdk.api.exactlyonce.manager.TransactionManager;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.store.ReadOffsetType;
import org.apache.rocketmq.sdk.shade.client.impl.consumer.ProcessQueue;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class OffsetUtil {
    private static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();

    public static Long getMQSafeOffset(MessageQueue mq, String consumerGroup) {
        DefaultMQPushConsumer consumer;
        if (mq == null || (consumer = TransactionManager.getConsumer(consumerGroup).iterator().next()) == null) {
            return null;
        }
        return Long.valueOf(consumer.getDefaultMQPushConsumerImpl().getOffsetStore().readOffset(mq, ReadOffsetType.READ_FROM_STORE) - ((long) (consumer.getPullThresholdForQueue() * 2)));
    }

    public static Set<MessageQueue> getMessageQueue(String consumerGroup, String topic) {
        Set<MessageQueue> mqAll = new HashSet<>();
        Set<DefaultMQPushConsumer> consumerSet = TransactionManager.getConsumer(consumerGroup);
        if (consumerSet == null || consumerSet.isEmpty()) {
            return mqAll;
        }
        for (DefaultMQPushConsumer consumer : consumerSet) {
            try {
                Set<MessageQueue> mqSet = consumer.getDefaultMQPushConsumerImpl().fetchSubscribeMessageQueues(topic);
                if (mqSet != null) {
                    mqAll.addAll(mqSet);
                }
            } catch (Exception e) {
                LogUtil.error(LOGGER, "fetchSubscribeMessageQueues fail, topic:{}, err:{}", topic, e.getMessage());
            }
        }
        return mqAll;
    }

    public static List<MessageQueue> getCurrentConsumeQueue(String consumerGroup) {
        Set<DefaultMQPushConsumer> consumerSet = TransactionManager.getConsumer(consumerGroup);
        if (consumerSet == null) {
            return null;
        }
        List<MessageQueue> mqAll = new ArrayList<>();
        for (DefaultMQPushConsumer consumer : consumerSet) {
            try {
                ConcurrentMap<MessageQueue, ProcessQueue> processTable = consumer.getDefaultMQPushConsumerImpl().getRebalanceImpl().getProcessQueueTable();
                if (processTable != null) {
                    mqAll.addAll(processTable.keySet());
                }
            } catch (Exception e) {
                LogUtil.error(LOGGER, "getProcessQueueTable fail, consumerGroup:{}, err:{}", consumer.getConsumerGroup(), e.getMessage());
            }
        }
        return mqAll;
    }
}
