package org.apache.rocketmq.sdk.shade.client;

import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class MQHelper {
    public static void resetOffsetByTimestamp(MessageModel messageModel, String consumerGroup, String topic, long timestamp) throws Exception {
        resetOffsetByTimestamp(messageModel, "DEFAULT", consumerGroup, topic, timestamp);
    }

    public static void resetOffsetByTimestamp(MessageModel messageModel, String instanceName, String consumerGroup, String topic, long timestamp) throws Exception {
        InternalLogger log = ClientLogger.getLog();
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(consumerGroup);
        consumer.setInstanceName(instanceName);
        consumer.setMessageModel(messageModel);
        consumer.start();
        Set<MessageQueue> mqs = null;
        try {
            try {
                mqs = consumer.fetchSubscribeMessageQueues(topic);
                if (mqs != null && !mqs.isEmpty()) {
                    Iterator<MessageQueue> it = new TreeSet<>(mqs).iterator();
                    while (it.hasNext()) {
                        MessageQueue mq = it.next();
                        long offset = consumer.searchOffset(mq, timestamp);
                        if (offset >= 0) {
                            consumer.updateConsumeOffset(mq, offset);
                            log.info("resetOffsetByTimestamp updateConsumeOffset success, {} {} {}", consumerGroup, Long.valueOf(offset), mq);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("resetOffsetByTimestamp Exception", (Throwable) e);
                throw e;
            }
        } finally {
            if (mqs != null) {
                consumer.getDefaultMQPullConsumerImpl().getOffsetStore().persistAll(mqs);
            }
            consumer.shutdown();
        }
    }
}
