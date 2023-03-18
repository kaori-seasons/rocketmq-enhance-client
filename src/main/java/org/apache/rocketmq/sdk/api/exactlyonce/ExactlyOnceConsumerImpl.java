package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.InternalCallback;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.ProxyTxExecuter;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.MetricService;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.TransactionManager;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.TxContextUtil;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.rocketmq.RMQConsumerAbstract;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.consumer.reporter.ConsumerStatusReporter;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ExactlyOnceConsumerImpl extends RMQConsumerAbstract implements ExactlyOnceConsumer {
    private static final String TRUE = "true";
    private final ConcurrentHashMap<String, MessageListener> subscribeTable = new ConcurrentHashMap<>();
    private boolean exactlyOnceDelivery;

    public ExactlyOnceConsumerImpl(Properties properties) {
        super(properties);
        this.exactlyOnceDelivery = false;
        String suspendTimeMillis = properties.getProperty(PropertyKeyConst.SuspendTimeMillis);
        if (!UtilAll.isBlank(suspendTimeMillis)) {
            try {
                this.defaultMQPushConsumer.setSuspendCurrentQueueTimeMillis(Long.parseLong(suspendTimeMillis));
            } catch (NumberFormatException e) {
            }
        }
        this.defaultMQPushConsumer.setConsumerStatusReporter(new ExactlyOnceConsumerStatusReporter());
        this.exactlyOnceDelivery = Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "true"));
        this.exactlyOnceDelivery = this.exactlyOnceDelivery && Boolean.parseBoolean(System.getProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "true"));
        if (this.exactlyOnceDelivery) {
            TransactionManager.start(properties);
        }
    }

    @Override
    public void start() {
        this.defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) new MessageConsumerExactlyOnceImpl());
        super.start();
        registerConsumerToManager();
    }

    private void registerConsumerToManager() {
        TransactionManager.addConsumer(this.defaultMQPushConsumer.getConsumerGroup(), this.defaultMQPushConsumer);
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

    class MessageConsumerExactlyOnceImpl implements MessageListenerConcurrently {
        MessageConsumerExactlyOnceImpl() {
        }

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msglist, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
            Action action;
            MessageExt msgRMQ = msglist.get(0);
            String consumerGroup = ExactlyOnceConsumerImpl.this.defaultMQPushConsumer.getConsumerGroup();
            String internalMsgId = TxContextUtil.buildInternalMsgId(msgRMQ, consumerGroup);
            final Message msg = RMQUtil.msgConvert(msgRMQ);
            msg.setMsgID(msgRMQ.getMsgId());
            final MessageListener listener = (MessageListener) ExactlyOnceConsumerImpl.this.subscribeTable.get(msg.getTopic());
            if (null == listener) {
                throw new RMQClientException("MessageListener is null");
            }
            final ConsumeContext context = new ConsumeContext();
            MQTxContext txContext = LocalTxContext.get();
            if (txContext == null) {
                txContext = new MQTxContext();
            }
            txContext.setMessageId(internalMsgId);
            txContext.setConsumerGroup(consumerGroup);
            txContext.setOffset(Long.valueOf(msgRMQ.getQueueOffset()));
            txContext.setTopicName(consumeConcurrentlyContext.getMessageQueue().getTopic());
            txContext.setMessageExt(msgRMQ);
            txContext.setMessageQueue(consumeConcurrentlyContext.getMessageQueue());
            LocalTxContext.set(txContext);
            if (ExactlyOnceConsumerImpl.this.exactlyOnceDelivery) {
                action = (Action) ProxyTxExecuter.getInstance().excute(new InternalCallback() {
                    @Override
                    public Object run() {
                        return listener.consume(msg, context);
                    }
                });
            } else {
                action = listener.consume(msg, context);
            }
            MetricService.getInstance().record(txContext);
            consumeConcurrentlyContext.setCheckSendBackHook(new ExactlyOnceCheckSendbackHook(consumerGroup, txContext.getDataSourceConfig()));
            consumeConcurrentlyContext.setExactlyOnceStatus(TxContextUtil.getExactlyOnceStatus(txContext));
            LocalTxContext.clear();
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

    class ExactlyOnceConsumerStatusReporter implements ConsumerStatusReporter {
        ExactlyOnceConsumerStatusReporter() {
        }

        @Override
        public Map<String, String> reportStatus() {
            return MetricService.getInstance().getCurrentConsumeStatus();
        }
    }
}
