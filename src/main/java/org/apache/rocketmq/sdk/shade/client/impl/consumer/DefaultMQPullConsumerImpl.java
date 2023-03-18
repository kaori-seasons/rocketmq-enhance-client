package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.QueryResult;
import org.apache.rocketmq.sdk.shade.client.Validators;
import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.PullCallback;
import org.apache.rocketmq.sdk.shade.client.consumer.PullResult;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.store.LocalFileOffsetStore;
import org.apache.rocketmq.sdk.shade.client.consumer.store.OffsetStore;
import org.apache.rocketmq.sdk.shade.client.consumer.store.ReadOffsetType;
import org.apache.rocketmq.sdk.shade.client.consumer.store.RemoteBrokerOffsetStore;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.sdk.shade.client.hook.FilterMessageHook;
import org.apache.rocketmq.sdk.shade.client.impl.CommunicationMode;
import org.apache.rocketmq.sdk.shade.client.impl.MQClientManager;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.ServiceState;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.filter.FilterAPI;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.common.sysflag.PullSysFlag;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class DefaultMQPullConsumerImpl implements MQConsumerInner {
    private final InternalLogger log = ClientLogger.getLog();
    private final DefaultMQPullConsumer defaultMQPullConsumer;
    private final long consumerStartTimestamp = System.currentTimeMillis();
    private final RPCHook rpcHook;
    private final ArrayList<ConsumeMessageHook> consumeMessageHookList = new ArrayList();
    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList();
    private volatile ServiceState serviceState;
    private MQClientInstance mQClientFactory;
    private PullAPIWrapper pullAPIWrapper;
    private OffsetStore offsetStore;
    private RebalanceImpl rebalanceImpl;

    public DefaultMQPullConsumerImpl(DefaultMQPullConsumer defaultMQPullConsumer, RPCHook rpcHook) {
        this.serviceState = ServiceState.CREATE_JUST;
        this.rebalanceImpl = new RebalancePullImpl(this);
        this.defaultMQPullConsumer = defaultMQPullConsumer;
        this.rpcHook = rpcHook;
    }

    public void registerConsumeMessageHook(ConsumeMessageHook hook) {
        this.consumeMessageHookList.add(hook);
        this.log.info("register consumeMessageHook Hook, {}", hook.hookName());
    }

    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        this.createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        this.makeSureStateOK();
        this.mQClientFactory.getMQAdminImpl().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    private void makeSureStateOK() throws MQClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new MQClientException("The consumer service state not OK, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    public long fetchConsumeOffset(MessageQueue mq, boolean fromStore) throws MQClientException {
        this.makeSureStateOK();
        return this.offsetStore.readOffset(mq, fromStore ? ReadOffsetType.READ_FROM_STORE : ReadOffsetType.MEMORY_FIRST_THEN_STORE);
    }

    public Set<MessageQueue> fetchMessageQueuesInBalance(String topic) throws MQClientException {
        this.makeSureStateOK();
        if (null == topic) {
            throw new IllegalArgumentException("topic is null");
        } else {
            ConcurrentMap<MessageQueue, ProcessQueue> mqTable = this.rebalanceImpl.getProcessQueueTable();
            Set<MessageQueue> mqResult = new HashSet();
            Iterator var4 = mqTable.keySet().iterator();

            while(var4.hasNext()) {
                MessageQueue mq = (MessageQueue)var4.next();
                if (mq.getTopic().equals(topic)) {
                    mqResult.add(mq);
                }
            }

            return this.parseSubscribeMessageQueues(mqResult);
        }
    }

    public Set<MessageQueue> parseSubscribeMessageQueues(Set<MessageQueue> queueSet) {
        Set<MessageQueue> resultQueues = new HashSet();
        Iterator var3 = queueSet.iterator();

        while(var3.hasNext()) {
            MessageQueue messageQueue = (MessageQueue)var3.next();
            String userTopic = NamespaceUtil.withoutNamespace(messageQueue.getTopic(), this.defaultMQPullConsumer.getNamespace());
            resultQueues.add(new MessageQueue(userTopic, messageQueue.getBrokerName(), messageQueue.getQueueId()));
        }

        return resultQueues;
    }

    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().fetchPublishMessageQueues(topic);
    }

    public Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().fetchSubscribeMessageQueues(topic);
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().earliestMsgStoreTime(mq);
    }

    public long maxOffset(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
    }

    public long minOffset(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().minOffset(mq);
    }

    public PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.pull(mq, subExpression, offset, maxNums, this.defaultMQPullConsumer.getConsumerPullTimeoutMillis());
    }

    public PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.pullSyncImpl(mq, subExpression, offset, maxNums, false, timeout);
    }

    private PullResult pullSyncImpl(MessageQueue mq, String subExpression, long offset, int maxNums, boolean block, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        this.makeSureStateOK();
        if (null == mq) {
            throw new MQClientException("mq is null", (Throwable)null);
        } else if (offset < 0L) {
            throw new MQClientException("offset < 0", (Throwable)null);
        } else if (maxNums <= 0) {
            throw new MQClientException("maxNums <= 0", (Throwable)null);
        } else {
            this.subscriptionAutomatically(mq.getTopic());
            int sysFlag = PullSysFlag.buildSysFlag(false, block, true, false);

            SubscriptionData subscriptionData;
            try {
                subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPullConsumer.getConsumerGroup(), mq.getTopic(), subExpression);
            } catch (Exception var15) {
                throw new MQClientException("parse subscription error", var15);
            }

            long timeoutMillis = block ? this.defaultMQPullConsumer.getConsumerTimeoutMillisWhenSuspend() : timeout;
            PullResult pullResult = this.pullAPIWrapper.pullKernelImpl(mq, subscriptionData.getSubString(), 0L, offset, maxNums, sysFlag, 0L, this.defaultMQPullConsumer.getBrokerSuspendMaxTimeMillis(), timeoutMillis, CommunicationMode.SYNC, (PullCallback)null);
            this.pullAPIWrapper.processPullResult(mq, pullResult, subscriptionData);
            this.resetTopic(pullResult.getMsgFoundList());
            if (!this.consumeMessageHookList.isEmpty()) {
                ConsumeMessageContext consumeMessageContext = null;
                consumeMessageContext = new ConsumeMessageContext();
                consumeMessageContext.setNamespace(this.defaultMQPullConsumer.getNamespace());
                consumeMessageContext.setConsumerGroup(this.groupName());
                consumeMessageContext.setMq(mq);
                consumeMessageContext.setMsgList(pullResult.getMsgFoundList());
                consumeMessageContext.setSuccess(false);
                this.executeHookBefore(consumeMessageContext);
                consumeMessageContext.setStatus(ConsumeConcurrentlyStatus.CONSUME_SUCCESS.toString());
                consumeMessageContext.setSuccess(true);
                this.executeHookAfter(consumeMessageContext);
            }

            return pullResult;
        }
    }

    public void resetTopic(List<MessageExt> msgList) {
        if (null != msgList && msgList.size() != 0) {
            Iterator var2 = msgList.iterator();

            while(var2.hasNext()) {
                MessageExt messageExt = (MessageExt)var2.next();
                if (null != this.getDefaultMQPullConsumer().getNamespace()) {
                    messageExt.setTopic(NamespaceUtil.withoutNamespace(messageExt.getTopic(), this.defaultMQPullConsumer.getNamespace()));
                }
            }

        }
    }

    public void subscriptionAutomatically(String topic) {
        if (!this.rebalanceImpl.getSubscriptionInner().containsKey(topic)) {
            try {
                SubscriptionData subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPullConsumer.getConsumerGroup(), topic, "*");
                this.rebalanceImpl.subscriptionInner.putIfAbsent(topic, subscriptionData);
            } catch (Exception var3) {
            }
        }

    }

    public void unsubscribe(String topic) {
        this.rebalanceImpl.getSubscriptionInner().remove(topic);
    }

    public String groupName() {
        return this.defaultMQPullConsumer.getConsumerGroup();
    }

    public void executeHookBefore(ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            Iterator var2 = this.consumeMessageHookList.iterator();

            while(var2.hasNext()) {
                ConsumeMessageHook hook = (ConsumeMessageHook)var2.next();

                try {
                    hook.consumeMessageBefore(context);
                } catch (Throwable var5) {
                }
            }
        }

    }

    public void executeHookAfter(ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            Iterator var2 = this.consumeMessageHookList.iterator();

            while(var2.hasNext()) {
                ConsumeMessageHook hook = (ConsumeMessageHook)var2.next();

                try {
                    hook.consumeMessageAfter(context);
                } catch (Throwable var5) {
                }
            }
        }

    }

    public MessageModel messageModel() {
        return this.defaultMQPullConsumer.getMessageModel();
    }

    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_ACTIVELY;
    }

    public ConsumeFromWhere consumeFromWhere() {
        return ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
    }

    public Set<SubscriptionData> subscriptions() {
        Set<SubscriptionData> result = new HashSet();
        Set<String> topics = this.defaultMQPullConsumer.getRegisterTopics();
        if (topics != null) {
            synchronized(topics) {
                Iterator var4 = topics.iterator();

                while(var4.hasNext()) {
                    String t = (String)var4.next();
                    SubscriptionData ms = null;

                    try {
                        ms = FilterAPI.buildSubscriptionData(this.groupName(), t, "*");
                    } catch (Exception var9) {
                        this.log.error("parse subscription error", var9);
                    }

                    if (ms != null) {
                        ms.setSubVersion(0L);
                        result.add(ms);
                    }
                }
            }
        }

        return result;
    }

    public void doRebalance() {
        if (this.rebalanceImpl != null) {
            this.rebalanceImpl.doRebalance(false);
        }

    }

    public void persistConsumerOffset() {
        try {
            this.makeSureStateOK();
            Set<MessageQueue> mqs = new HashSet();
            Set<MessageQueue> allocateMq = this.rebalanceImpl.getProcessQueueTable().keySet();
            mqs.addAll(allocateMq);
            this.offsetStore.persistAll(mqs);
        } catch (Exception var3) {
            this.log.error("group: " + this.defaultMQPullConsumer.getConsumerGroup() + " persistConsumerOffset exception", var3);
        }

    }

    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {
        Map<String, SubscriptionData> subTable = this.rebalanceImpl.getSubscriptionInner();
        if (subTable != null && subTable.containsKey(topic)) {
            this.rebalanceImpl.getTopicSubscribeInfoTable().put(topic, info);
        }

    }

    public boolean isSubscribeTopicNeedUpdate(String topic) {
        Map<String, SubscriptionData> subTable = this.rebalanceImpl.getSubscriptionInner();
        if (subTable != null && subTable.containsKey(topic)) {
            return !this.rebalanceImpl.topicSubscribeInfoTable.containsKey(topic);
        } else {
            return false;
        }
    }

    public boolean isUnitMode() {
        return this.defaultMQPullConsumer.isUnitMode();
    }

    public ConsumerRunningInfo consumerRunningInfo() {
        ConsumerRunningInfo info = new ConsumerRunningInfo();
        Properties prop = MixAll.object2Properties(this.defaultMQPullConsumer);
        prop.put("PROP_CONSUMER_START_TIMESTAMP", String.valueOf(this.consumerStartTimestamp));
        info.setProperties(prop);
        info.getSubscriptionSet().addAll(this.subscriptions());
        return info;
    }

    public void pull(MessageQueue mq, String subExpression, long offset, int maxNums, PullCallback pullCallback) throws MQClientException, RemotingException, InterruptedException {
        this.pull(mq, subExpression, offset, maxNums, pullCallback, this.defaultMQPullConsumer.getConsumerPullTimeoutMillis());
    }

    public void pull(MessageQueue mq, String subExpression, long offset, int maxNums, PullCallback pullCallback, long timeout) throws MQClientException, RemotingException, InterruptedException {
        this.pullAsyncImpl(mq, subExpression, offset, maxNums, pullCallback, false, timeout);
    }

    private void pullAsyncImpl(final MessageQueue mq, String subExpression, long offset, int maxNums, final PullCallback pullCallback, boolean block, long timeout) throws MQClientException, RemotingException, InterruptedException {
        this.makeSureStateOK();
        if (null == mq) {
            throw new MQClientException("mq is null", (Throwable)null);
        } else if (offset < 0L) {
            throw new MQClientException("offset < 0", (Throwable)null);
        } else if (maxNums <= 0) {
            throw new MQClientException("maxNums <= 0", (Throwable)null);
        } else if (null == pullCallback) {
            throw new MQClientException("pullCallback is null", (Throwable)null);
        } else {
            this.subscriptionAutomatically(mq.getTopic());

            try {
                int sysFlag = PullSysFlag.buildSysFlag(false, block, true, false);

                final SubscriptionData subscriptionData;
                try {
                    subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPullConsumer.getConsumerGroup(), mq.getTopic(), subExpression);
                } catch (Exception var14) {
                    throw new MQClientException("parse subscription error", var14);
                }

                long timeoutMillis = block ? this.defaultMQPullConsumer.getConsumerTimeoutMillisWhenSuspend() : timeout;
                this.pullAPIWrapper.pullKernelImpl(mq, subscriptionData.getSubString(), 0L, offset, maxNums, sysFlag, 0L, this.defaultMQPullConsumer.getBrokerSuspendMaxTimeMillis(), timeoutMillis, CommunicationMode.ASYNC, new PullCallback() {
                    public void onSuccess(PullResult pullResult) {
                        pullCallback.onSuccess(DefaultMQPullConsumerImpl.this.pullAPIWrapper.processPullResult(mq, pullResult, subscriptionData));
                    }

                    public void onException(Throwable e) {
                        pullCallback.onException(e);
                    }
                });
            } catch (MQBrokerException var15) {
                throw new MQClientException("pullAsync unknow exception", var15);
            }
        }
    }

    public PullResult pullBlockIfNotFound(MessageQueue mq, String subExpression, long offset, int maxNums) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.pullSyncImpl(mq, subExpression, offset, maxNums, true, this.getDefaultMQPullConsumer().getConsumerPullTimeoutMillis());
    }

    public DefaultMQPullConsumer getDefaultMQPullConsumer() {
        return this.defaultMQPullConsumer;
    }

    public void pullBlockIfNotFound(MessageQueue mq, String subExpression, long offset, int maxNums, PullCallback pullCallback) throws MQClientException, RemotingException, InterruptedException {
        this.pullAsyncImpl(mq, subExpression, offset, maxNums, pullCallback, true, this.getDefaultMQPullConsumer().getConsumerPullTimeoutMillis());
    }

    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().queryMessage(topic, key, maxNum, begin, end);
    }

    public MessageExt queryMessageByUniqKey(String topic, String uniqKey) throws MQClientException, InterruptedException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().queryMessageByUniqKey(topic, uniqKey);
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().searchOffset(mq, timestamp);
    }

    public void sendMessageBack(MessageExt msg, int delayLevel, String brokerName) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        this.sendMessageBack(msg, delayLevel, brokerName, this.defaultMQPullConsumer.getConsumerGroup());
    }

    public void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        this.offsetStore.updateConsumeOffsetToBroker(mq, offset, isOneway);
    }

    public void sendMessageBack(MessageExt msg, int delayLevel, String brokerName, String consumerGroup) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        try {
            String brokerAddr = null != brokerName ? this.mQClientFactory.findBrokerAddressInPublish(brokerName) : RemotingHelper.parseSocketAddressAddr(msg.getStoreHost());
            if (UtilAll.isBlank(consumerGroup)) {
                consumerGroup = this.defaultMQPullConsumer.getConsumerGroup();
            }

            if (brokerAddr == null) {
                brokerAddr = RemotingHelper.parseSocketAddressAddr(msg.getStoreHost());
            }

            this.mQClientFactory.getMQClientAPIImpl().consumerSendMessageBack(brokerAddr, msg, consumerGroup, delayLevel, 3000L, this.defaultMQPullConsumer.getMaxReconsumeTimes());
        } catch (Exception var11) {
            this.log.error("sendMessageBack Exception, " + this.defaultMQPullConsumer.getConsumerGroup(), var11);
            Message newMsg = new Message(MixAll.getRetryTopic(this.defaultMQPullConsumer.getConsumerGroup()), msg.getBody());
            String originMsgId = MessageAccessor.getOriginMessageId(msg);
            MessageAccessor.setOriginMessageId(newMsg, UtilAll.isBlank(originMsgId) ? msg.getMsgId() : originMsgId);
            newMsg.setFlag(msg.getFlag());
            MessageAccessor.setProperties(newMsg, msg.getProperties());
            MessageAccessor.putProperty(newMsg, "RETRY_TOPIC", msg.getTopic());
            MessageAccessor.setReconsumeTime(newMsg, String.valueOf(msg.getReconsumeTimes() + 1));
            MessageAccessor.setMaxReconsumeTimes(newMsg, String.valueOf(this.defaultMQPullConsumer.getMaxReconsumeTimes()));
            newMsg.setDelayTimeLevel(3 + msg.getReconsumeTimes());
            this.mQClientFactory.getDefaultMQProducer().send(newMsg);
        } finally {
            msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQPullConsumer.getNamespace()));
        }

    }

    public synchronized void shutdown() {
        switch(this.serviceState) {
            case RUNNING:
                this.persistConsumerOffset();
                this.mQClientFactory.unregisterConsumer(this.defaultMQPullConsumer.getConsumerGroup());
                this.mQClientFactory.shutdown();
                this.log.info("the consumer [{}] shutdown OK", this.defaultMQPullConsumer.getConsumerGroup());
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
            case CREATE_JUST:
            case SHUTDOWN_ALREADY:
            default:
        }
    }

    public synchronized void start() throws MQClientException {
        switch(this.serviceState) {
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;
                this.checkConfig();
                this.copySubscription();
                if (this.defaultMQPullConsumer.getMessageModel() == MessageModel.CLUSTERING) {
                    this.defaultMQPullConsumer.changeInstanceNameToPID();
                }

                this.mQClientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultMQPullConsumer, this.rpcHook);
                this.rebalanceImpl.setConsumerGroup(this.defaultMQPullConsumer.getConsumerGroup());
                this.rebalanceImpl.setMessageModel(this.defaultMQPullConsumer.getMessageModel());
                this.rebalanceImpl.setAllocateMessageQueueStrategy(this.defaultMQPullConsumer.getAllocateMessageQueueStrategy());
                this.rebalanceImpl.setmQClientFactory(this.mQClientFactory);
                this.pullAPIWrapper = new PullAPIWrapper(this.mQClientFactory, this.defaultMQPullConsumer.getConsumerGroup(), this.isUnitMode());
                this.pullAPIWrapper.registerFilterMessageHook(this.filterMessageHookList);
                if (this.defaultMQPullConsumer.getOffsetStore() != null) {
                    this.offsetStore = this.defaultMQPullConsumer.getOffsetStore();
                } else {
                    switch(this.defaultMQPullConsumer.getMessageModel()) {
                        case BROADCASTING:
                            this.offsetStore = new LocalFileOffsetStore(this.mQClientFactory, this.defaultMQPullConsumer.getConsumerGroup());
                            break;
                        case CLUSTERING:
                            this.offsetStore = new RemoteBrokerOffsetStore(this.mQClientFactory, this.defaultMQPullConsumer.getConsumerGroup());
                    }

                    this.defaultMQPullConsumer.setOffsetStore(this.offsetStore);
                }

                this.offsetStore.load();
                boolean registerOK = this.mQClientFactory.registerConsumer(this.defaultMQPullConsumer.getConsumerGroup(), this);
                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;
                    throw new MQClientException("The consumer group[" + this.defaultMQPullConsumer.getConsumerGroup() + "] has been created before, specify another name please." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                } else {
                    this.mQClientFactory.start();
                    this.log.info("the consumer [{}] start OK", this.defaultMQPullConsumer.getConsumerGroup());
                    this.serviceState = ServiceState.RUNNING;
                }
            default:
                return;
            case RUNNING:
            case SHUTDOWN_ALREADY:
            case START_FAILED:
                throw new MQClientException("The PullConsumer service state not OK, maybe started once, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    private void checkConfig() throws MQClientException {
        Validators.checkGroup(this.defaultMQPullConsumer.getConsumerGroup());
        if (null == this.defaultMQPullConsumer.getConsumerGroup()) {
            throw new MQClientException("consumerGroup is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (this.defaultMQPullConsumer.getConsumerGroup().equals("DEFAULT_CONSUMER")) {
            throw new MQClientException("consumerGroup can not equal DEFAULT_CONSUMER, please specify another one." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (null == this.defaultMQPullConsumer.getMessageModel()) {
            throw new MQClientException("messageModel is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (null == this.defaultMQPullConsumer.getAllocateMessageQueueStrategy()) {
            throw new MQClientException("allocateMessageQueueStrategy is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (this.defaultMQPullConsumer.getConsumerTimeoutMillisWhenSuspend() < this.defaultMQPullConsumer.getBrokerSuspendMaxTimeMillis()) {
            throw new MQClientException("Long polling mode, the consumer consumerTimeoutMillisWhenSuspend must greater than brokerSuspendMaxTimeMillis" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    private void copySubscription() throws MQClientException {
        try {
            Set<String> registerTopics = this.defaultMQPullConsumer.getRegisterTopics();
            if (registerTopics != null) {
                Iterator var2 = registerTopics.iterator();

                while(var2.hasNext()) {
                    String topic = (String)var2.next();
                    SubscriptionData subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPullConsumer.getConsumerGroup(), topic, "*");
                    this.rebalanceImpl.getSubscriptionInner().put(topic, subscriptionData);
                }
            }

        } catch (Exception var5) {
            throw new MQClientException("subscription exception", var5);
        }
    }

    public void updateConsumeOffset(MessageQueue mq, long offset) throws MQClientException {
        this.makeSureStateOK();
        this.offsetStore.updateOffset(mq, offset, false);
    }

    public MessageExt viewMessage(String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().viewMessage(msgId);
    }

    public void registerFilterMessageHook(FilterMessageHook hook) {
        this.filterMessageHookList.add(hook);
        this.log.info("register FilterMessageHook Hook, {}", hook.hookName());
    }

    public OffsetStore getOffsetStore() {
        return this.offsetStore;
    }

    public void setOffsetStore(OffsetStore offsetStore) {
        this.offsetStore = offsetStore;
    }

    public PullAPIWrapper getPullAPIWrapper() {
        return this.pullAPIWrapper;
    }

    public void setPullAPIWrapper(PullAPIWrapper pullAPIWrapper) {
        this.pullAPIWrapper = pullAPIWrapper;
    }

    public ServiceState getServiceState() {
        return this.serviceState;
    }

    /** @deprecated */
    @Deprecated
    public void setServiceState(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public long getConsumerStartTimestamp() {
        return this.consumerStartTimestamp;
    }

    public RebalanceImpl getRebalanceImpl() {
        return this.rebalanceImpl;
    }
}
