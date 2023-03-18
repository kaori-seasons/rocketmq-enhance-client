package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.batch.BatchConsumer;
import org.apache.rocketmq.sdk.api.batch.BatchMessageListener;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class BatchConsumerImpl extends RMQConsumerAbstract implements BatchConsumer {
    private static final int MAX_BATCH_SIZE = 32;
    private static final int MIN_BATCH_SIZE = 1;
    private final ConcurrentHashMap<String, BatchMessageListener> subscribeTable = new ConcurrentHashMap<>();

    public BatchConsumerImpl(Properties properties) {
        super(properties);
        this.defaultMQPushConsumer.setPostSubscriptionWhenPull(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.PostSubscriptionWhenPull, "false")));
        this.defaultMQPushConsumer.setMessageModel(MessageModel.valueOf(properties.getProperty(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING)));
        String consumeBatchSize = properties.getProperty(PropertyKeyConst.ConsumeMessageBatchMaxSize);
        if (!UtilAll.isBlank(consumeBatchSize)) {
            this.defaultMQPushConsumer.setConsumeMessageBatchMaxSize(Math.max(1, Math.min(32, Integer.valueOf(consumeBatchSize).intValue())));
        }
        String timedBatchConsumeAwaitDuration = properties.getProperty(PropertyKeyConst.BatchConsumeMaxAwaitDurationInSeconds);
        if (!UtilAll.isBlank(timedBatchConsumeAwaitDuration)) {
            try {
                this.defaultMQPushConsumer.setMaxBatchConsumeWaitTime(Long.parseLong(timedBatchConsumeAwaitDuration), TimeUnit.SECONDS);
            } catch (MQClientException e) {
                log.error("Invalid value for BatchConsumeMaxAwaitDurationInSeconds", (Throwable) e);
            } catch (NumberFormatException e2) {
                log.error("Number format error", (Throwable) e2);
            }
        }
    }

    @Override
    public void start() {
        this.defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) new BatchMessageListenerImpl(this.subscribeTable));
        super.start();
    }

    @Override
    public void subscribe(String topic, String subExpression, BatchMessageListener listener) {
        if (null == topic) {
            throw new RMQClientException("topic is null");
        } else if (null == listener) {
            throw new RMQClientException("listener is null");
        } else {
            this.subscribeTable.put(topic, listener);
            super.subscribe(topic, subExpression);
        }
    }

    @Override
    public void unsubscribe(String topic) {
        if (null != topic) {
            this.subscribeTable.remove(topic);
            super.unsubscribe(topic);
        }
    }

    class BatchMessageListenerImpl implements MessageListenerConcurrently {
        private final ConcurrentMap<String, BatchMessageListener> subscribeTable;

        public BatchMessageListenerImpl(ConcurrentMap<String, BatchMessageListener> subscribeTable) {
            this.subscribeTable = subscribeTable;
        }

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> rmqMsgList, ConsumeConcurrentlyContext contextRMQ) {
            List<Message> msgList = new ArrayList<>();
            for (MessageExt rmqMsg : rmqMsgList) {
                Message msg = RMQUtil.msgConvert(rmqMsg);
                Map<String, String> propertiesMap = rmqMsg.getProperties();
                msg.setMsgID(rmqMsg.getMsgId());
                if (!(propertiesMap == null || propertiesMap.get(Constants.TRANSACTION_ID) == null)) {
                    msg.setMsgID(propertiesMap.get(Constants.TRANSACTION_ID));
                }
                msgList.add(msg);
            }
            BatchMessageListener listener = this.subscribeTable.get(msgList.get(0).getTopic());
            if (null == listener) {
                throw new RMQClientException("BatchMessageListener is null");
            }
            Action action = listener.consume(msgList, new ConsumeContext());
            if (action == null) {
                return null;
            }
            switch (action) {
                case CommitMessage:
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                case ReconsumeLater:
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                default:
                    return null;
            }
        }
    }
}
