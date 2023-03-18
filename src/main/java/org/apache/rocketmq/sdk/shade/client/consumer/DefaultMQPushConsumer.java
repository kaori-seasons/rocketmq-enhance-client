package org.apache.rocketmq.sdk.shade.client.consumer;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import org.apache.rocketmq.sdk.shade.client.ClientConfig;
import org.apache.rocketmq.sdk.shade.client.QueryResult;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListener;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.sdk.shade.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.sdk.shade.client.consumer.reporter.ConsumerStatusReporter;
import org.apache.rocketmq.sdk.shade.client.consumer.store.OffsetStore;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.ResponseCode;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.api.MessageSelector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DefaultMQPushConsumer extends ClientConfig implements MQPushConsumer {
    protected final transient DefaultMQPushConsumerImpl defaultMQPushConsumerImpl;
    private String consumerGroup;
    private MessageModel messageModel;
    private ConsumeFromWhere consumeFromWhere;
    private String consumeTimestamp;
    private AllocateMessageQueueStrategy allocateMessageQueueStrategy;
    private Map<String, String> subscription;
    private MessageListener messageListener;
    private ConsumerStatusReporter consumerStatusReporter;
    private OffsetStore offsetStore;
    private int consumeThreadMin;
    private int consumeThreadMax;
    private long adjustThreadPoolNumsThreshold;
    private int consumeConcurrentlyMaxSpan;
    private int pullThresholdForQueue;
    private int pullThresholdSizeForQueue;
    private int pullThresholdForTopic;
    private int pullThresholdSizeForTopic;
    private long pullInterval;
    private int consumeMessageBatchMaxSize;
    private long maxBatchConsumeWaitTime;
    private int pullBatchSize;
    private boolean postSubscriptionWhenPull;
    private boolean unitMode;
    private int maxReconsumeTimes;
    private long suspendCurrentQueueTimeMillis;
    private long consumeTimeout;

    public DefaultMQPushConsumer() {
        this(null, MixAll.DEFAULT_CONSUMER_GROUP, null, new AllocateMessageQueueAveragely());
    }

    public DefaultMQPushConsumer(String namespace, String consumerGroup) {
        this(namespace, consumerGroup, null, new AllocateMessageQueueAveragely());
    }

    public DefaultMQPushConsumer(String namespace, String consumerGroup, RPCHook rpcHook) {
        this(namespace, consumerGroup, rpcHook, new AllocateMessageQueueAveragely());
    }

    public DefaultMQPushConsumer(String consumerGroup, RPCHook rpcHook, AllocateMessageQueueStrategy allocateMessageQueueStrategy) {
        this(null, consumerGroup, rpcHook, allocateMessageQueueStrategy);
    }

    public DefaultMQPushConsumer(String namespace, String consumerGroup, RPCHook rpcHook, AllocateMessageQueueStrategy allocateMessageQueueStrategy) {
        this.messageModel = MessageModel.CLUSTERING;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.consumeTimestamp = UtilAll.timeMillisToHumanString3(System.currentTimeMillis() - DruidAbstractDataSource.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
        this.subscription = new HashMap();
        this.consumeThreadMin = 20;
        this.consumeThreadMax = 64;
        this.adjustThreadPoolNumsThreshold = 100000;
        this.consumeConcurrentlyMaxSpan = 2000;
        this.pullThresholdForQueue = 1000;
        this.pullThresholdSizeForQueue = 100;
        this.pullThresholdForTopic = -1;
        this.pullThresholdSizeForTopic = -1;
        this.pullInterval = 0;
        this.consumeMessageBatchMaxSize = 1;
        this.maxBatchConsumeWaitTime = 0;
        this.pullBatchSize = 32;
        this.postSubscriptionWhenPull = false;
        this.unitMode = false;
        this.maxReconsumeTimes = -1;
        this.suspendCurrentQueueTimeMillis = 1000;
        this.consumeTimeout = 15;
        this.consumerGroup = consumerGroup;
        this.namespace = namespace;
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
        this.defaultMQPushConsumerImpl = new DefaultMQPushConsumerImpl(this, rpcHook);
    }

    public DefaultMQPushConsumer(RPCHook rpcHook) {
        this(null, MixAll.DEFAULT_CONSUMER_GROUP, rpcHook, new AllocateMessageQueueAveragely());
    }

    public DefaultMQPushConsumer(String consumerGroup) {
        this(null, consumerGroup, null, new AllocateMessageQueueAveragely());
    }

    @Override 
    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    @Override 
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        this.defaultMQPushConsumerImpl.createTopic(key, withNamespace(newTopic), queueNum, topicSysFlag);
    }

    @Override 
    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return this.defaultMQPushConsumerImpl.searchOffset(queueWithNamespace(mq), timestamp);
    }

    @Override 
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return this.defaultMQPushConsumerImpl.maxOffset(queueWithNamespace(mq));
    }

    @Override 
    public long minOffset(MessageQueue mq) throws MQClientException {
        return this.defaultMQPushConsumerImpl.minOffset(queueWithNamespace(mq));
    }

    @Override 
    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return this.defaultMQPushConsumerImpl.earliestMsgStoreTime(queueWithNamespace(mq));
    }

    @Override 
    public MessageExt viewMessage(String offsetMsgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return this.defaultMQPushConsumerImpl.viewMessage(offsetMsgId);
    }

    @Override 
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        return this.defaultMQPushConsumerImpl.queryMessage(withNamespace(topic), key, maxNum, begin, end);
    }

    @Override 
    public MessageExt viewMessage(String topic, String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        try {
            MessageDecoder.decodeMessageId(msgId);
            return viewMessage(msgId);
        } catch (Exception e) {
            return this.defaultMQPushConsumerImpl.queryMessageByUniqKey(withNamespace(topic), msgId);
        }
    }

    public AllocateMessageQueueStrategy getAllocateMessageQueueStrategy() {
        return this.allocateMessageQueueStrategy;
    }

    public void setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy allocateMessageQueueStrategy) {
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
    }

    public int getConsumeConcurrentlyMaxSpan() {
        return this.consumeConcurrentlyMaxSpan;
    }

    public void setConsumeConcurrentlyMaxSpan(int consumeConcurrentlyMaxSpan) {
        this.consumeConcurrentlyMaxSpan = consumeConcurrentlyMaxSpan;
    }

    public ConsumeFromWhere getConsumeFromWhere() {
        return this.consumeFromWhere;
    }

    public void setConsumeFromWhere(ConsumeFromWhere consumeFromWhere) {
        this.consumeFromWhere = consumeFromWhere;
    }

    public int getConsumeMessageBatchMaxSize() {
        return this.consumeMessageBatchMaxSize;
    }

    public void setConsumeMessageBatchMaxSize(int consumeMessageBatchMaxSize) {
        this.consumeMessageBatchMaxSize = consumeMessageBatchMaxSize;
    }

    public long getMaxBatchConsumeWaitTime() {
        return this.maxBatchConsumeWaitTime;
    }

    public void setMaxBatchConsumeWaitTime(long amount, TimeUnit timeUnit) throws MQClientException {
        if (amount >= 0) {
            if (null == timeUnit) {
                this.maxBatchConsumeWaitTime = amount;
                return;
            }
            this.maxBatchConsumeWaitTime = TimeUnit.MILLISECONDS.convert(amount, timeUnit);
            if (this.maxBatchConsumeWaitTime > TimeUnit.MILLISECONDS.convert(this.consumeTimeout, TimeUnit.MINUTES) / 2) {
                throw new MQClientException((int) ResponseCode.BAD_CONFIGURATION, "Batch await time should not exceed half of consume timeout");
            }
        }
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public int getConsumeThreadMax() {
        return this.consumeThreadMax;
    }

    public void setConsumeThreadMax(int consumeThreadMax) {
        this.consumeThreadMax = consumeThreadMax;
    }

    public int getConsumeThreadMin() {
        return this.consumeThreadMin;
    }

    public void setConsumeThreadMin(int consumeThreadMin) {
        this.consumeThreadMin = consumeThreadMin;
    }

    public DefaultMQPushConsumerImpl getDefaultMQPushConsumerImpl() {
        return this.defaultMQPushConsumerImpl;
    }

    public MessageListener getMessageListener() {
        return this.messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public ConsumerStatusReporter getConsumerStatusReporter() {
        return this.consumerStatusReporter;
    }

    public void setConsumerStatusReporter(ConsumerStatusReporter consumerStatusReporter) {
        this.consumerStatusReporter = consumerStatusReporter;
    }

    public MessageModel getMessageModel() {
        return this.messageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public int getPullBatchSize() {
        return this.pullBatchSize;
    }

    public void setPullBatchSize(int pullBatchSize) {
        this.pullBatchSize = pullBatchSize;
    }

    public long getPullInterval() {
        return this.pullInterval;
    }

    public void setPullInterval(long pullInterval) {
        this.pullInterval = pullInterval;
    }

    public int getPullThresholdForQueue() {
        return this.pullThresholdForQueue;
    }

    public void setPullThresholdForQueue(int pullThresholdForQueue) {
        this.pullThresholdForQueue = pullThresholdForQueue;
    }

    public int getPullThresholdForTopic() {
        return this.pullThresholdForTopic;
    }

    public void setPullThresholdForTopic(int pullThresholdForTopic) {
        this.pullThresholdForTopic = pullThresholdForTopic;
    }

    public int getPullThresholdSizeForQueue() {
        return this.pullThresholdSizeForQueue;
    }

    public void setPullThresholdSizeForQueue(int pullThresholdSizeForQueue) {
        this.pullThresholdSizeForQueue = pullThresholdSizeForQueue;
    }

    public int getPullThresholdSizeForTopic() {
        return this.pullThresholdSizeForTopic;
    }

    public void setPullThresholdSizeForTopic(int pullThresholdSizeForTopic) {
        this.pullThresholdSizeForTopic = pullThresholdSizeForTopic;
    }

    public Map<String, String> getSubscription() {
        return this.subscription;
    }

    public void setSubscription(Map<String, String> subscription) {
        Map<String, String> subscriptionWithNamespace = new HashMap<>();
        for (String topic : subscription.keySet()) {
            subscriptionWithNamespace.put(withNamespace(topic), subscription.get(topic));
        }
        this.subscription = subscriptionWithNamespace;
    }

    @Override 
    public void sendMessageBack(MessageExt msg, int delayLevel) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.defaultMQPushConsumerImpl.sendMessageBack(msg, delayLevel, null);
    }

    @Override 
    public void sendMessageBack(MessageExt msg, int delayLevel, String brokerName) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.defaultMQPushConsumerImpl.sendMessageBack(msg, delayLevel, brokerName);
    }

    @Override 
    public Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException {
        return this.defaultMQPushConsumerImpl.fetchSubscribeMessageQueues(withNamespace(topic));
    }

    @Override 
    public void start() throws MQClientException {
        setConsumerGroup(NamespaceUtil.wrapNamespace(getNamespace(), this.consumerGroup));
        this.defaultMQPushConsumerImpl.start();
    }

    @Override 
    public void shutdown() {
        this.defaultMQPushConsumerImpl.shutdown();
    }

    @Override 
    @Deprecated
    public void registerMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
        this.defaultMQPushConsumerImpl.registerMessageListener(messageListener);
    }

    @Override 
    public void registerMessageListener(MessageListenerConcurrently messageListener) {
        this.messageListener = messageListener;
        this.defaultMQPushConsumerImpl.registerMessageListener(messageListener);
    }

    @Override 
    public void registerMessageListener(MessageListenerOrderly messageListener) {
        this.messageListener = messageListener;
        this.defaultMQPushConsumerImpl.registerMessageListener(messageListener);
    }

    @Override 
    public void subscribe(String topic, String subExpression) throws MQClientException {
        this.defaultMQPushConsumerImpl.subscribe(withNamespace(topic), subExpression);
    }

    @Override 
    public void subscribe(String topic, String fullClassName, String filterClassSource) throws MQClientException {
        this.defaultMQPushConsumerImpl.subscribe(withNamespace(topic), fullClassName, filterClassSource);
    }

    @Override 
    public void subscribe(String topic, MessageSelector messageSelector) throws MQClientException {
        this.defaultMQPushConsumerImpl.subscribe(withNamespace(topic), messageSelector);
    }

    @Override 
    public void unsubscribe(String topic) {
        this.defaultMQPushConsumerImpl.unsubscribe(withNamespace(topic));
    }

    @Override 
    public void updateCorePoolSize(int corePoolSize) {
        this.defaultMQPushConsumerImpl.updateCorePoolSize(corePoolSize);
    }

    @Override 
    public void suspend() {
        this.defaultMQPushConsumerImpl.suspend();
    }

    @Override 
    public void resume() {
        this.defaultMQPushConsumerImpl.resume();
    }

    public OffsetStore getOffsetStore() {
        return this.offsetStore;
    }

    public void setOffsetStore(OffsetStore offsetStore) {
        this.offsetStore = offsetStore;
    }

    public String getConsumeTimestamp() {
        return this.consumeTimestamp;
    }

    public void setConsumeTimestamp(String consumeTimestamp) {
        this.consumeTimestamp = consumeTimestamp;
    }

    public boolean isPostSubscriptionWhenPull() {
        return this.postSubscriptionWhenPull;
    }

    public void setPostSubscriptionWhenPull(boolean postSubscriptionWhenPull) {
        this.postSubscriptionWhenPull = postSubscriptionWhenPull;
    }

    @Override
    public boolean isUnitMode() {
        return this.unitMode;
    }

    @Override
    public void setUnitMode(boolean isUnitMode) {
        this.unitMode = isUnitMode;
    }

    public long getAdjustThreadPoolNumsThreshold() {
        return this.adjustThreadPoolNumsThreshold;
    }

    public void setAdjustThreadPoolNumsThreshold(long adjustThreadPoolNumsThreshold) {
        this.adjustThreadPoolNumsThreshold = adjustThreadPoolNumsThreshold;
    }

    public int getMaxReconsumeTimes() {
        return this.maxReconsumeTimes;
    }

    public void setMaxReconsumeTimes(int maxReconsumeTimes) {
        this.maxReconsumeTimes = maxReconsumeTimes;
    }

    public long getSuspendCurrentQueueTimeMillis() {
        return this.suspendCurrentQueueTimeMillis;
    }

    public void setSuspendCurrentQueueTimeMillis(long suspendCurrentQueueTimeMillis) {
        this.suspendCurrentQueueTimeMillis = suspendCurrentQueueTimeMillis;
    }

    public long getConsumeTimeout() {
        return this.consumeTimeout;
    }

    public void setConsumeTimeout(long consumeTimeout) {
        this.consumeTimeout = consumeTimeout;
    }
}
