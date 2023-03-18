package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.api.order.ConsumeOrderContext;
import org.apache.rocketmq.sdk.api.order.MessageOrderListener;
import org.apache.rocketmq.sdk.api.order.OrderAction;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class OrderConsumerImpl extends RMQConsumerAbstract implements OrderConsumer {
    private final ConcurrentHashMap<String, MessageOrderListener> subscribeTable = new ConcurrentHashMap<>();

    public OrderConsumerImpl(Properties properties) {
        super(properties);
        String suspendTimeMillis = properties.getProperty(PropertyKeyConst.SuspendTimeMillis);
        if (!UtilAll.isBlank(suspendTimeMillis)) {
            try {
                this.defaultMQPushConsumer.setSuspendCurrentQueueTimeMillis(Long.parseLong(suspendTimeMillis));
            } catch (NumberFormatException e) {
            }
        }
    }

    @Override
    public void start() {
        this.defaultMQPushConsumer.registerMessageListener((MessageListenerOrderly) new MessageListenerOrderlyImpl());
        super.start();
    }

    @Override
    public void subscribe(String topic, String subExpression, MessageOrderListener listener) {
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
    public void subscribe(String topic, MessageSelector selector, MessageOrderListener listener) {
        if (null == topic) {
            throw new RMQClientException("topic is null");
        } else if (null == listener) {
            throw new RMQClientException("listener is null");
        } else {
            this.subscribeTable.put(topic, listener);
            super.subscribe(topic, selector);
        }
    }

    class MessageListenerOrderlyImpl implements MessageListenerOrderly {
        MessageListenerOrderlyImpl() {
        }

        @Override
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> arg0, ConsumeOrderlyContext arg1) {
            MessageExt msgRMQ = arg0.get(0);
            Message msg = RMQUtil.msgConvert(msgRMQ);
            msg.setMsgID(msgRMQ.getMsgId());
            MessageOrderListener listener = (MessageOrderListener) OrderConsumerImpl.this.subscribeTable.get(msg.getTopic());
            if (null == listener) {
                throw new RMQClientException("MessageOrderListener is null");
            }
            OrderAction action = listener.consume(msg, new ConsumeOrderContext());
            if (action != null) {
                switch (action) {
                    case Success:
                        return ConsumeOrderlyStatus.SUCCESS;
                    case Suspend:
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        }
    }
}
