package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerImpl extends RMQConsumerAbstract implements Consumer {
    private final ConcurrentHashMap<String, MessageListener> subscribeTable = new ConcurrentHashMap<>();

    public ConsumerImpl(Properties properties) {
        super(properties);
        this.defaultMQPushConsumer.setPostSubscriptionWhenPull(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.PostSubscriptionWhenPull, "false")));
        this.defaultMQPushConsumer.setMessageModel(MessageModel.valueOf(properties.getProperty(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING)));
        this.defaultMQPushConsumer.setUseTLS(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isTlsEnable, "false")));
    }

    @Override
    public void start() {
        this.defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) new MessageListenerImpl());
        super.start();
    }

    @Override
    public void subscribe(String topic, String subExpression, MessageListener listener) {
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
    public void subscribe(String topic, MessageSelector selector, MessageListener listener) {
        if (null == topic) {
            throw new RMQClientException("topic is null");
        } else if (null == listener) {
            throw new RMQClientException("listener is null");
        } else {
            this.subscribeTable.put(topic, listener);
            super.subscribe(topic, selector);
        }
    }

    @Override
    public void unsubscribe(String topic) {
        if (null != topic) {
            this.subscribeTable.remove(topic);
            super.unsubscribe(topic);
        }
    }

    class MessageListenerImpl implements MessageListenerConcurrently {
        MessageListenerImpl() {
        }

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgsRMQList, ConsumeConcurrentlyContext contextRMQ) {
            MessageExt msgRMQ = msgsRMQList.get(0);
            Message msg = RMQUtil.msgConvert(msgRMQ);
            Map<String, String> stringStringMap = msgRMQ.getProperties();
            msg.setMsgID(msgRMQ.getMsgId());
            if (!(stringStringMap == null || stringStringMap.get(Constants.TRANSACTION_ID) == null)) {
                msg.setMsgID(stringStringMap.get(Constants.TRANSACTION_ID));
            }
            MessageListener listener = (MessageListener) ConsumerImpl.this.subscribeTable.get(msg.getTopic());
            if (null == listener) {
                throw new RMQClientException("MessageListener is null");
            }
            Action action = listener.consume(msg, new ConsumeContext());
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
