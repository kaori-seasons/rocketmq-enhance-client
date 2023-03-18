package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.consumer.PullResultExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import java.util.HashMap;
import java.util.Map;

public class PullConsumerTest {
    private static final Map<MessageQueue, Long> offsetTable = new HashMap();

    public static void main(String[] args) throws Exception {
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("consumer_test");
        consumer.setNamesrvAddr("10.154.0.61:9876");
        consumer.start();
        try {
            for (MessageQueue mq : consumer.fetchSubscribeMessageQueues("topic_test")) {
                System.out.println("Consume from the queue: " + mq);
                PullResultExt pullResult = (PullResultExt) consumer.pullBlockIfNotFound(mq, "*", getMessageQueueOffset(mq), 32);
                putMessageQueueOffset(mq, pullResult.getNextBeginOffset());
                switch (pullResult.getPullStatus()) {
                    case FOUND:
                        for (MessageExt m : pullResult.getMsgFoundList()) {
                            System.out.println(m.toString());
                        }
                        break;
                }
            }
        } catch (MQClientException e) {
        }
    }

    private static void putMessageQueueOffset(MessageQueue mq, long offset) {
        offsetTable.put(mq, Long.valueOf(offset));
    }

    private static long getMessageQueueOffset(MessageQueue mq) {
        Long offset = offsetTable.get(mq);
        if (offset != null) {
            return offset.longValue();
        }
        return 0;
    }
}
