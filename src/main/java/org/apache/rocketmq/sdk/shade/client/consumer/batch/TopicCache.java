package org.apache.rocketmq.sdk.shade.client.consumer.batch;

import org.apache.rocketmq.sdk.shade.client.impl.consumer.ConsumeMessageConcurrentlyService;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class TopicCache {
    private static final InternalLogger LOGGER = ClientLogger.getLog();
    private final String topic;
    private final ConcurrentMap<MessageQueue, PriorityBlockingQueue<MessageExt>> cache = new ConcurrentHashMap();

    public TopicCache(String topic) {
        this.topic = topic;
    }

    public int size() {
        int total = 0;
        for (Map.Entry<MessageQueue, PriorityBlockingQueue<MessageExt>> entry : this.cache.entrySet()) {
            total += entry.getValue().size();
        }
        return total;
    }

    public void put(Collection<MessageExt> messages) {
        for (MessageExt message : messages) {
            if (!this.topic.equals(message.getTopic())) {
                LOGGER.warn("Trying to put an message with mismatched topic name, expect: {}, actual: {}, actualFQN: {}", this.topic, message.getTopic(), ConsumeMessageConcurrentlyService.topicOf(message));
            } else {
                MessageQueue messageQueue = new MessageQueue(ConsumeMessageConcurrentlyService.topicOf(message), message.getBrokerName(), message.getQueueId());
                if (!this.cache.containsKey(messageQueue)) {
                    this.cache.putIfAbsent(messageQueue, new PriorityBlockingQueue<>(128, new Comparator<MessageExt>() {
                        public int compare(MessageExt o1, MessageExt o2) {
                            long lhs = o1.getQueueOffset();
                            long rhs = o2.getQueueOffset();
                            if (lhs < rhs) {
                                return -1;
                            }
                            return lhs == rhs ? 0 : 1;
                        }
                    }));
                }
                this.cache.get(messageQueue).add(message);
            }
        }
    }

    public boolean take(int count, Collection<MessageExt> collection) {
        if (size() < count) {
            return false;
        }
        int remain = count;
        while (remain > 0) {
            MessageExt candidate = null;
            PriorityBlockingQueue<MessageExt> targetQueue = null;
            for (Map.Entry<MessageQueue, PriorityBlockingQueue<MessageExt>> entry : this.cache.entrySet()) {
                PriorityBlockingQueue<MessageExt> messages = entry.getValue();
                if (null == candidate) {
                    candidate = messages.peek();
                    targetQueue = messages;
                } else {
                    MessageExt challenger = messages.peek();
                    if (null != challenger && challenger.getDecodedTime() < candidate.getDecodedTime()) {
                        candidate = challenger;
                        targetQueue = messages;
                    }
                }
            }
            if (null != candidate) {
                collection.add(targetQueue.poll());
                remain--;
            }
        }
        return true;
    }

    public long elapsed() {
        long earliest = System.currentTimeMillis();
        Iterator cacheIterator = this.cache.entrySet().iterator();

        while(cacheIterator.hasNext()) {
            Map.Entry<MessageQueue, PriorityBlockingQueue<MessageExt>> entry = (Map.Entry)cacheIterator.next();
            MessageExt message = (MessageExt)((PriorityBlockingQueue)entry.getValue()).peek();
            if (null != message) {
                long decodedTime = message.getDecodedTime();
                if (decodedTime < earliest) {
                    earliest = decodedTime;
                }
            }
        }

        return earliest - earliest;
    }
}
