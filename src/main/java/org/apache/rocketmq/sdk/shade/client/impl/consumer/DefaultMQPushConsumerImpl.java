package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.QueryResult;
import org.apache.rocketmq.sdk.shade.client.Validators;
import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.PullCallback;
import org.apache.rocketmq.sdk.shade.client.consumer.PullResult;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListener;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerOrderly;
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
import org.apache.rocketmq.sdk.shade.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.ServiceState;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.filter.FilterAPI;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeStatus;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ProcessQueueInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.body.QueueTimeSpan;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.BrokerData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.sdk.shade.common.sysflag.PullSysFlag;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.api.MessageSelector;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.sdk.shade.common.message.*;
import org.springframework.util.backoff.FixedBackOff;

public class DefaultMQPushConsumerImpl implements MQConsumerInner {
    private static final long PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION = 3000L;
    private static final long PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL = 50L;
    private static final long PULL_TIME_DELAY_MILLS_WHEN_SUSPEND = 1000L;
    private static final long BROKER_SUSPEND_MAX_TIME_MILLIS = 15000L;
    private static final long CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND = 30000L;
    private final InternalLogger log = ClientLogger.getLog();
    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final RebalanceImpl rebalanceImpl = new RebalancePushImpl(this);
    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList();
    private final long consumerStartTimestamp = System.currentTimeMillis();
    private final ArrayList<ConsumeMessageHook> consumeMessageHookList = new ArrayList();
    private final RPCHook rpcHook;
    private volatile ServiceState serviceState;
    private MQClientInstance mQClientFactory;
    private PullAPIWrapper pullAPIWrapper;
    private volatile boolean pause;
    private boolean consumeOrderly;
    private MessageListener messageListenerInner;
    private OffsetStore offsetStore;
    private ConsumeMessageService consumeMessageService;
    private long queueFlowControlTimes;
    private long queueMaxSpanFlowControlTimes;

    public DefaultMQPushConsumerImpl(DefaultMQPushConsumer defaultMQPushConsumer, RPCHook rpcHook) {
        this.serviceState = ServiceState.CREATE_JUST;
        this.pause = false;
        this.consumeOrderly = false;
        this.queueFlowControlTimes = 0L;
        this.queueMaxSpanFlowControlTimes = 0L;
        this.defaultMQPushConsumer = defaultMQPushConsumer;
        this.rpcHook = rpcHook;
    }

    public void registerFilterMessageHook(FilterMessageHook hook) {
        this.filterMessageHookList.add(hook);
        this.log.info("register FilterMessageHook Hook, {}", hook.hookName());
    }

    public boolean hasHook() {
        return !this.consumeMessageHookList.isEmpty();
    }

    public void registerConsumeMessageHook(ConsumeMessageHook hook) {
        this.consumeMessageHookList.add(hook);
        this.log.info("register consumeMessageHook Hook, {}", hook.hookName());
    }

    public void executeHookBefore(ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            Iterator<ConsumeMessageHook> it = this.consumeMessageHookList.iterator();
            while (it.hasNext()) {
                try {
                    it.next().consumeMessageBefore(context);
                } catch (Throwable th) {
                }
            }
        }

    }

    public void executeHookAfter(ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            Iterator<ConsumeMessageHook> it = this.consumeMessageHookList.iterator();
            while (it.hasNext()) {
                try {
                    it.next().consumeMessageAfter(context);
                } catch (Throwable th) {
                }
            }
        }
    }

    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        this.createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        this.mQClientFactory.getMQAdminImpl().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    public Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException {
        Set<MessageQueue> result = this.rebalanceImpl.getTopicSubscribeInfoTable().get(topic);
        if (null == result) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
            result = this.rebalanceImpl.getTopicSubscribeInfoTable().get(topic);
        }
        if (null != result) {
            return parseSubscribeMessageQueues(result);
        }
        throw new MQClientException("The topic[" + topic + "] not exist", (Throwable) null);
    }

    public Set<MessageQueue> parseSubscribeMessageQueues(Set<MessageQueue> messageQueueList) {
        Set<MessageQueue> resultQueues = new HashSet<>();
        for (MessageQueue queue : messageQueueList) {
            resultQueues.add(new MessageQueue(NamespaceUtil.withoutNamespace(queue.getTopic(), this.defaultMQPushConsumer.getNamespace()), queue.getBrokerName(), queue.getQueueId()));
        }
        return resultQueues;
    }

    public DefaultMQPushConsumer getDefaultMQPushConsumer() {
        return this.defaultMQPushConsumer;
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return this.mQClientFactory.getMQAdminImpl().earliestMsgStoreTime(mq);
    }

    public long maxOffset(MessageQueue mq) throws MQClientException {
        return this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
    }

    public long minOffset(MessageQueue mq) throws MQClientException {
        return this.mQClientFactory.getMQAdminImpl().minOffset(mq);
    }

    public OffsetStore getOffsetStore() {
        return this.offsetStore;
    }

    public void setOffsetStore(OffsetStore offsetStore) {
        this.offsetStore = offsetStore;
    }

    public void pullMessage(final PullRequest pullRequest) {
        final ProcessQueue processQueue = pullRequest.getProcessQueue();
        if (processQueue.isDropped()) {
            this.log.info("the pull request[{}] is dropped.", pullRequest.toString());
        } else {
            pullRequest.getProcessQueue().setLastPullTimestamp(System.currentTimeMillis());

            try {
                this.makeSureStateOK();
            } catch (MQClientException var20) {
                this.log.warn("pullMessage exception, consumer state not ok", var20);
                this.executePullRequestLater(pullRequest, 3000L);
                return;
            }

            if (this.isPause()) {
                this.log.warn("consumer was paused, execute pull request later. instanceName={}, group={}", this.defaultMQPushConsumer.getInstanceName(), this.defaultMQPushConsumer.getConsumerGroup());
                this.executePullRequestLater(pullRequest, 1000L);
            } else {
                long cachedMessageCount = processQueue.getMsgCount().get();
                long cachedMessageSizeInMiB = processQueue.getMsgSize().get() / 1048576L;
                if (cachedMessageCount > (long)this.defaultMQPushConsumer.getPullThresholdForQueue()) {
                    this.executePullRequestLater(pullRequest, 50L);
                    if (this.queueFlowControlTimes++ % 1000L == 0L) {
                        this.log.warn("the cached message count exceeds the threshold {}, so do flow control, minOffset={}, maxOffset={}, count={}, size={} MiB, pullRequest={}, flowControlTimes={}", new Object[]{this.defaultMQPushConsumer.getPullThresholdForQueue(), processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), cachedMessageCount, cachedMessageSizeInMiB, pullRequest, this.queueFlowControlTimes});
                    }

                } else if (cachedMessageSizeInMiB > (long)this.defaultMQPushConsumer.getPullThresholdSizeForQueue()) {
                    this.executePullRequestLater(pullRequest, 50L);
                    if (this.queueFlowControlTimes++ % 1000L == 0L) {
                        this.log.warn("the cached message size exceeds the threshold {} MiB, so do flow control, minOffset={}, maxOffset={}, count={}, size={} MiB, pullRequest={}, flowControlTimes={}", new Object[]{this.defaultMQPushConsumer.getPullThresholdSizeForQueue(), processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), cachedMessageCount, cachedMessageSizeInMiB, pullRequest, this.queueFlowControlTimes});
                    }

                } else {
                    if (!this.consumeOrderly) {
                        if (processQueue.getMaxSpan() > (long)this.defaultMQPushConsumer.getConsumeConcurrentlyMaxSpan()) {
                            this.executePullRequestLater(pullRequest, 50L);
                            if (this.queueMaxSpanFlowControlTimes++ % 1000L == 0L) {
                                this.log.warn("the queue's messages, span too long, so do flow control, minOffset={}, maxOffset={}, maxSpan={}, pullRequest={}, flowControlTimes={}", new Object[]{processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), processQueue.getMaxSpan(), pullRequest, this.queueMaxSpanFlowControlTimes});
                            }

                            return;
                        }
                    } else {
                        if (!processQueue.isLocked()) {
                            this.executePullRequestLater(pullRequest, 3000L);
                            this.log.info("pull message later because not locked in broker, {}", pullRequest);
                            return;
                        }

                        if (!pullRequest.isLockedFirst()) {
                            long offset = this.rebalanceImpl.computePullFromWhere(pullRequest.getMessageQueue());
                            boolean brokerBusy = offset < pullRequest.getNextOffset();
                            this.log.info("the first time to pull message, so fix offset from broker. pullRequest: {} NewOffset: {} brokerBusy: {}", new Object[]{pullRequest, offset, brokerBusy});
                            if (brokerBusy) {
                                this.log.info("[NOTIFYME]the first time to pull message, but pull request offset larger than broker consume offset. pullRequest: {} NewOffset: {}", pullRequest, offset);
                            }

                            pullRequest.setLockedFirst(true);
                            pullRequest.setNextOffset(offset);
                        }
                    }

                    final SubscriptionData subscriptionData = (SubscriptionData)this.rebalanceImpl.getSubscriptionInner().get(pullRequest.getMessageQueue().getTopic());
                    if (null == subscriptionData) {
                        this.executePullRequestLater(pullRequest, 3000L);
                        this.log.warn("find the consumer's subscription failed, {}", pullRequest);
                    } else {
                        final long beginTimestamp = System.currentTimeMillis();
                        PullCallback pullCallback = new PullCallback() {
                            public void onSuccess(PullResult pullResult) {
                                if (pullResult != null) {
                                    pullResult = DefaultMQPushConsumerImpl.this.pullAPIWrapper.processPullResult(pullRequest.getMessageQueue(), pullResult, subscriptionData);
                                    switch (pullResult.getPullStatus()) {
                                        case FOUND:
                                            long prevRequestOffset = pullRequest.getNextOffset();
                                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());
                                            long pullRT = System.currentTimeMillis() - beginTimestamp;
                                            DefaultMQPushConsumerImpl.this.getConsumerStatsManager().incPullRT(pullRequest.getConsumerGroup(), pullRequest.getMessageQueue().getTopic(), pullRT);
                                            long firstMsgOffset = Long.MAX_VALUE;
                                            if (pullResult.getMsgFoundList() != null && !pullResult.getMsgFoundList().isEmpty()) {
                                                firstMsgOffset = ((MessageExt)pullResult.getMsgFoundList().get(0)).getQueueOffset();
                                                DefaultMQPushConsumerImpl.this.getConsumerStatsManager().incPullTPS(pullRequest.getConsumerGroup(), pullRequest.getMessageQueue().getTopic(), (long)pullResult.getMsgFoundList().size());
                                                boolean dispatchToConsume = processQueue.putMessage(pullResult.getMsgFoundList());
                                                DefaultMQPushConsumerImpl.this.consumeMessageService.submitConsumeRequest(pullResult.getMsgFoundList(), processQueue, pullRequest.getMessageQueue(), dispatchToConsume);
                                                if (DefaultMQPushConsumerImpl.this.defaultMQPushConsumer.getPullInterval() > 0L) {
                                                    DefaultMQPushConsumerImpl.this.executePullRequestLater(pullRequest, DefaultMQPushConsumerImpl.this.defaultMQPushConsumer.getPullInterval());
                                                } else {
                                                    DefaultMQPushConsumerImpl.this.executePullRequestImmediately(pullRequest);
                                                }
                                            } else {
                                                DefaultMQPushConsumerImpl.this.executePullRequestImmediately(pullRequest);
                                            }

                                            if (pullResult.getNextBeginOffset() < prevRequestOffset || firstMsgOffset < prevRequestOffset) {
                                                DefaultMQPushConsumerImpl.this.log.warn("[BUG] pull message result maybe data wrong, nextBeginOffset: {} firstMsgOffset: {} prevRequestOffset: {}", new Object[]{pullResult.getNextBeginOffset(), firstMsgOffset, prevRequestOffset});
                                            }
                                            break;
                                        case NO_NEW_MSG:
                                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());
                                            DefaultMQPushConsumerImpl.this.correctTagsOffset(pullRequest);
                                            DefaultMQPushConsumerImpl.this.executePullRequestImmediately(pullRequest);
                                            break;
                                        case NO_MATCHED_MSG:
                                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());
                                            DefaultMQPushConsumerImpl.this.correctTagsOffset(pullRequest);
                                            DefaultMQPushConsumerImpl.this.executePullRequestImmediately(pullRequest);
                                            break;
                                        case OFFSET_ILLEGAL:
                                            DefaultMQPushConsumerImpl.this.log.warn("the pull request offset illegal, {} {}", pullRequest.toString(), pullResult.toString());
                                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());
                                            pullRequest.getProcessQueue().setDropped(true);
                                            DefaultMQPushConsumerImpl.this.executeTaskLater(new Runnable() {
                                                public void run() {
                                                    try {
                                                        DefaultMQPushConsumerImpl.this.offsetStore.updateOffset(pullRequest.getMessageQueue(), pullRequest.getNextOffset(), false);
                                                        DefaultMQPushConsumerImpl.this.offsetStore.persist(pullRequest.getMessageQueue());
                                                        DefaultMQPushConsumerImpl.this.rebalanceImpl.removeProcessQueue(pullRequest.getMessageQueue());
                                                        DefaultMQPushConsumerImpl.this.log.warn("fix the pull request offset, {}", pullRequest);
                                                    } catch (Throwable var2) {
                                                        DefaultMQPushConsumerImpl.this.log.error("executeTaskLater Exception", var2);
                                                    }

                                                }
                                            }, 10000L);
                                    }
                                }

                            }

                            public void onException(Throwable e) {
                                if (!pullRequest.getMessageQueue().getTopic().startsWith("%RETRY%")) {
                                    DefaultMQPushConsumerImpl.this.log.warn("execute the pull request exception", e);
                                }

                                DefaultMQPushConsumerImpl.this.executePullRequestLater(pullRequest, 3000L);
                            }
                        };
                        boolean commitOffsetEnable = false;
                        long commitOffsetValue = 0L;
                        if (MessageModel.CLUSTERING == this.defaultMQPushConsumer.getMessageModel()) {
                            commitOffsetValue = this.offsetStore.readOffset(pullRequest.getMessageQueue(), ReadOffsetType.READ_FROM_MEMORY);
                            if (commitOffsetValue > 0L) {
                                commitOffsetEnable = true;
                            }
                        }

                        String subExpression = null;
                        boolean classFilter = false;
                        SubscriptionData sd = (SubscriptionData)this.rebalanceImpl.getSubscriptionInner().get(pullRequest.getMessageQueue().getTopic());
                        if (sd != null) {
                            if (this.defaultMQPushConsumer.isPostSubscriptionWhenPull() && !sd.isClassFilterMode()) {
                                subExpression = sd.getSubString();
                            }

                            classFilter = sd.isClassFilterMode();
                        }

                        int sysFlag = PullSysFlag.buildSysFlag(commitOffsetEnable, true, subExpression != null, classFilter);

                        try {
                            this.pullAPIWrapper.pullKernelImpl(pullRequest.getMessageQueue(), subExpression, subscriptionData.getExpressionType(), subscriptionData.getSubVersion(), pullRequest.getNextOffset(), this.defaultMQPushConsumer.getPullBatchSize(), sysFlag, commitOffsetValue, 15000L, 30000L, CommunicationMode.ASYNC, pullCallback);
                        } catch (Exception var19) {
                            this.log.error("pullKernelImpl exception", var19);
                            this.executePullRequestLater(pullRequest, 3000L);
                        }

                    }
                }
            }
        }
    }

    private void makeSureStateOK() throws MQClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new MQClientException("The consumer service state not OK, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable) null);
        }
    }

    public void executePullRequestLater(PullRequest pullRequest, long timeDelay) {
        this.mQClientFactory.getPullMessageService().executePullRequestLater(pullRequest, timeDelay);
    }

    public boolean isPause() {
        return this.pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return this.mQClientFactory.getConsumerStatsManager();
    }

    public void executePullRequestImmediately(PullRequest pullRequest) {
        this.mQClientFactory.getPullMessageService().executePullRequestImmediately(pullRequest);
    }

    public void correctTagsOffset(PullRequest pullRequest) {
        if (0 == pullRequest.getProcessQueue().getMsgCount().get()) {
            this.offsetStore.updateOffset(pullRequest.getMessageQueue(), pullRequest.getNextOffset(), true);
        }
    }

    public void executeTaskLater(Runnable r, long timeDelay) {
        this.mQClientFactory.getPullMessageService().executeTaskLater(r, timeDelay);
    }

    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        return this.mQClientFactory.getMQAdminImpl().queryMessage(topic, key, maxNum, begin, end);
    }

    public MessageExt queryMessageByUniqKey(String topic, String uniqKey) throws MQClientException, InterruptedException {
        return this.mQClientFactory.getMQAdminImpl().queryMessageByUniqKey(topic, uniqKey);
    }

    public void registerMessageListener(MessageListener messageListener) {
        this.messageListenerInner = messageListener;
    }

    public void resume() {
        this.pause = false;
        doRebalance();
        this.log.info("resume this consumer, {}", this.defaultMQPushConsumer.getConsumerGroup());
    }

    public void sendMessageBack(MessageExt msg, int delayLevel, String brokerName) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        try {
            try {
                this.mQClientFactory.getMQClientAPIImpl().consumerSendMessageBack(null != brokerName ? this.mQClientFactory.findBrokerAddressInPublish(brokerName) : RemotingHelper.parseSocketAddressAddr(msg.getStoreHost()), msg, this.defaultMQPushConsumer.getConsumerGroup(), delayLevel, FixedBackOff.DEFAULT_INTERVAL, getMaxReconsumeTimes());
                msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQPushConsumer.getNamespace()));
            } catch (Exception e) {
                this.log.error("sendMessageBack Exception, " + this.defaultMQPushConsumer.getConsumerGroup(), (Throwable) e);
                Message newMsg = new Message(MixAll.getRetryTopic(this.defaultMQPushConsumer.getConsumerGroup()), msg.getBody());
                String originMsgId = MessageAccessor.getOriginMessageId(msg);
                MessageAccessor.setOriginMessageId(newMsg, UtilAll.isBlank(originMsgId) ? msg.getMsgId() : originMsgId);
                newMsg.setFlag(msg.getFlag());
                MessageAccessor.setProperties(newMsg, msg.getProperties());
                MessageAccessor.putProperty(newMsg, MessageConst.PROPERTY_RETRY_TOPIC, msg.getTopic());
                MessageAccessor.setReconsumeTime(newMsg, String.valueOf(msg.getReconsumeTimes() + 1));
                MessageAccessor.setMaxReconsumeTimes(newMsg, String.valueOf(getMaxReconsumeTimes()));
                newMsg.setDelayTimeLevel(3 + msg.getReconsumeTimes());
                this.mQClientFactory.getDefaultMQProducer().send(newMsg);
                msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQPushConsumer.getNamespace()));
            }
        } catch (Throwable th) {
            msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQPushConsumer.getNamespace()));
            throw th;
        }
    }

    private int getMaxReconsumeTimes() {
        return this.defaultMQPushConsumer.getMaxReconsumeTimes() == -1 ? 16 : this.defaultMQPushConsumer.getMaxReconsumeTimes();
    }

    public synchronized void shutdown() {
        switch (this.serviceState) {
            case RUNNING:
                this.consumeMessageService.shutdown();
                this.persistConsumerOffset();
                this.mQClientFactory.unregisterConsumer(this.defaultMQPushConsumer.getConsumerGroup());
                this.mQClientFactory.shutdown();
                this.log.info("the consumer [{}] shutdown OK", this.defaultMQPushConsumer.getConsumerGroup());
                this.rebalanceImpl.destroy();
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
            case CREATE_JUST:
            case SHUTDOWN_ALREADY:
            default:
        }
    }

    public synchronized void start() throws MQClientException {
        switch (this.serviceState) {
            case CREATE_JUST:
                this.log.info("the consumer [{}] start beginning. messageModel={}, isUnitMode={}", new Object[]{this.defaultMQPushConsumer.getConsumerGroup(), this.defaultMQPushConsumer.getMessageModel(), this.defaultMQPushConsumer.isUnitMode()});
                this.serviceState = ServiceState.START_FAILED;
                this.checkConfig();
                this.copySubscription();
                if (this.defaultMQPushConsumer.getMessageModel() == MessageModel.CLUSTERING) {
                    this.defaultMQPushConsumer.changeInstanceNameToPID();
                }

                this.mQClientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultMQPushConsumer, this.rpcHook);
                this.rebalanceImpl.setConsumerGroup(this.defaultMQPushConsumer.getConsumerGroup());
                this.rebalanceImpl.setMessageModel(this.defaultMQPushConsumer.getMessageModel());
                this.rebalanceImpl.setAllocateMessageQueueStrategy(this.defaultMQPushConsumer.getAllocateMessageQueueStrategy());
                this.rebalanceImpl.setmQClientFactory(this.mQClientFactory);
                this.pullAPIWrapper = new PullAPIWrapper(this.mQClientFactory, this.defaultMQPushConsumer.getConsumerGroup(), this.isUnitMode());
                this.pullAPIWrapper.registerFilterMessageHook(this.filterMessageHookList);
                if (this.defaultMQPushConsumer.getOffsetStore() != null) {
                    this.offsetStore = this.defaultMQPushConsumer.getOffsetStore();
                } else {
                    switch (this.defaultMQPushConsumer.getMessageModel()) {
                        case BROADCASTING:
                            this.offsetStore = new LocalFileOffsetStore(this.mQClientFactory, this.defaultMQPushConsumer.getConsumerGroup());
                            break;
                        case CLUSTERING:
                            this.offsetStore = new RemoteBrokerOffsetStore(this.mQClientFactory, this.defaultMQPushConsumer.getConsumerGroup());
                    }

                    this.defaultMQPushConsumer.setOffsetStore(this.offsetStore);
                }

                this.offsetStore.load();
                if (this.getMessageListenerInner() instanceof MessageListenerOrderly) {
                    this.consumeOrderly = true;
                    this.consumeMessageService = new ConsumeMessageOrderlyService(this, (MessageListenerOrderly)this.getMessageListenerInner());
                } else if (this.getMessageListenerInner() instanceof MessageListenerConcurrently) {
                    this.consumeOrderly = false;
                    this.consumeMessageService = new ConsumeMessageConcurrentlyService(this, (MessageListenerConcurrently)this.getMessageListenerInner());
                }

                this.consumeMessageService.start();
                boolean registerOK = this.mQClientFactory.registerConsumer(this.defaultMQPushConsumer.getConsumerGroup(), this);
                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;
                    this.consumeMessageService.shutdown();
                    throw new MQClientException("The consumer group[" + this.defaultMQPushConsumer.getConsumerGroup() + "] has been created before, specify another name please." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                } else {
                    this.mQClientFactory.start();
                    this.log.info("the consumer [{}] start OK.", this.defaultMQPushConsumer.getConsumerGroup());
                    this.serviceState = ServiceState.RUNNING;
                }
            default:
                this.updateTopicSubscribeInfoWhenSubscriptionChanged();
                this.mQClientFactory.checkClientInBroker();
                this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
                this.mQClientFactory.rebalanceImmediately();
                return;
            case RUNNING:
            case SHUTDOWN_ALREADY:
            case START_FAILED:
                throw new MQClientException("The PushConsumer service state not OK, maybe started once, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    private void checkConfig() throws MQClientException {
        Validators.checkGroup(this.defaultMQPushConsumer.getConsumerGroup());
        if (null == this.defaultMQPushConsumer.getConsumerGroup()) {
            throw new MQClientException("consumerGroup is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (this.defaultMQPushConsumer.getConsumerGroup().equals("DEFAULT_CONSUMER")) {
            throw new MQClientException("consumerGroup can not equal DEFAULT_CONSUMER, please specify another one." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (null == this.defaultMQPushConsumer.getMessageModel()) {
            throw new MQClientException("messageModel is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else if (null == this.defaultMQPushConsumer.getConsumeFromWhere()) {
            throw new MQClientException("consumeFromWhere is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        } else {
            Date dt = UtilAll.parseDate(this.defaultMQPushConsumer.getConsumeTimestamp(), "yyyyMMddHHmmss");
            if (null == dt) {
                throw new MQClientException("consumeTimestamp is invalid, the valid format is yyyyMMddHHmmss,but received " + this.defaultMQPushConsumer.getConsumeTimestamp() + " " + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
            } else if (null == this.defaultMQPushConsumer.getAllocateMessageQueueStrategy()) {
                throw new MQClientException("allocateMessageQueueStrategy is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
            } else if (null == this.defaultMQPushConsumer.getSubscription()) {
                throw new MQClientException("subscription is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
            } else if (null == this.defaultMQPushConsumer.getMessageListener()) {
                throw new MQClientException("messageListener is null" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
            } else {
                boolean orderly = this.defaultMQPushConsumer.getMessageListener() instanceof MessageListenerOrderly;
                boolean concurrently = this.defaultMQPushConsumer.getMessageListener() instanceof MessageListenerConcurrently;
                if (!orderly && !concurrently) {
                    throw new MQClientException("messageListener must be instanceof MessageListenerOrderly or MessageListenerConcurrently" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                } else if (this.defaultMQPushConsumer.getConsumeThreadMin() >= 1 && this.defaultMQPushConsumer.getConsumeThreadMin() <= 1000) {
                    if (this.defaultMQPushConsumer.getConsumeThreadMax() >= 1 && this.defaultMQPushConsumer.getConsumeThreadMax() <= 1000) {
                        if (this.defaultMQPushConsumer.getConsumeThreadMin() > this.defaultMQPushConsumer.getConsumeThreadMax()) {
                            throw new MQClientException("consumeThreadMin (" + this.defaultMQPushConsumer.getConsumeThreadMin() + ") is larger than consumeThreadMax (" + this.defaultMQPushConsumer.getConsumeThreadMax() + ")", (Throwable)null);
                        } else if (this.defaultMQPushConsumer.getConsumeConcurrentlyMaxSpan() >= 1 && this.defaultMQPushConsumer.getConsumeConcurrentlyMaxSpan() <= 65535) {
                            if (this.defaultMQPushConsumer.getPullThresholdForQueue() >= 1 && this.defaultMQPushConsumer.getPullThresholdForQueue() <= 65535) {
                                if (this.defaultMQPushConsumer.getPullThresholdForTopic() != -1 && (this.defaultMQPushConsumer.getPullThresholdForTopic() < 1 || this.defaultMQPushConsumer.getPullThresholdForTopic() > 6553500)) {
                                    throw new MQClientException("pullThresholdForTopic Out of range [1, 6553500]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                } else if (this.defaultMQPushConsumer.getPullThresholdSizeForQueue() >= 1 && this.defaultMQPushConsumer.getPullThresholdSizeForQueue() <= 1024) {
                                    if (this.defaultMQPushConsumer.getPullThresholdSizeForTopic() != -1 && (this.defaultMQPushConsumer.getPullThresholdSizeForTopic() < 1 || this.defaultMQPushConsumer.getPullThresholdSizeForTopic() > 102400)) {
                                        throw new MQClientException("pullThresholdSizeForTopic Out of range [1, 102400]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                    } else if (this.defaultMQPushConsumer.getPullInterval() >= 0L && this.defaultMQPushConsumer.getPullInterval() <= 65535L) {
                                        if (this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize() >= 1 && this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize() <= 1024) {
                                            if (this.defaultMQPushConsumer.getPullBatchSize() < 1 || this.defaultMQPushConsumer.getPullBatchSize() > 1024) {
                                                throw new MQClientException("pullBatchSize Out of range [1, 1024]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                            }
                                        } else {
                                            throw new MQClientException("consumeMessageBatchMaxSize Out of range [1, 1024]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                        }
                                    } else {
                                        throw new MQClientException("pullInterval Out of range [0, 65535]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                    }
                                } else {
                                    throw new MQClientException("pullThresholdSizeForQueue Out of range [1, 1024]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                                }
                            } else {
                                throw new MQClientException("pullThresholdForQueue Out of range [1, 65535]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                            }
                        } else {
                            throw new MQClientException("consumeConcurrentlyMaxSpan Out of range [1, 65535]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                        }
                    } else {
                        throw new MQClientException("consumeThreadMax Out of range [1, 1000]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                    }
                } else {
                    throw new MQClientException("consumeThreadMin Out of range [1, 1000]" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                }
            }
        }
    }

    private void copySubscription() throws MQClientException {
        try {
            Map<String, String> sub = this.defaultMQPushConsumer.getSubscription();
            if (sub != null) {
                for (Map.Entry<String, String> entry : sub.entrySet()) {
                    String topic = entry.getKey();
                    this.rebalanceImpl.getSubscriptionInner().put(topic, FilterAPI.buildSubscriptionData(this.defaultMQPushConsumer.getConsumerGroup(), topic, entry.getValue()));
                }
            }
            if (null == this.messageListenerInner) {
                this.messageListenerInner = this.defaultMQPushConsumer.getMessageListener();
            }
            switch (this.defaultMQPushConsumer.getMessageModel()) {
                case BROADCASTING:
                    break;
                case CLUSTERING:
                    String retryTopic = MixAll.getRetryTopic(this.defaultMQPushConsumer.getConsumerGroup());
                    this.rebalanceImpl.getSubscriptionInner().put(retryTopic, FilterAPI.buildSubscriptionData(this.defaultMQPushConsumer.getConsumerGroup(), retryTopic, "*"));
                    break;
            }
        } catch (Exception e) {
            throw new MQClientException("subscription exception", e);
        }
    }

    public MessageListener getMessageListenerInner() {
        return this.messageListenerInner;
    }

    private void updateTopicSubscribeInfoWhenSubscriptionChanged() {
        Map<String, SubscriptionData> subTable = getSubscriptionInner();
        if (subTable != null) {
            for (Map.Entry<String, SubscriptionData> entry : subTable.entrySet()) {
                this.mQClientFactory.updateTopicRouteInfoFromNameServer(entry.getKey());
            }
        }
    }

    public ConcurrentMap<String, SubscriptionData> getSubscriptionInner() {
        return this.rebalanceImpl.getSubscriptionInner();
    }

    public void subscribe(String topic, String subExpression) throws MQClientException {
        try {
            SubscriptionData subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPushConsumer.getConsumerGroup(), topic, subExpression);
            this.rebalanceImpl.getSubscriptionInner().put(topic, subscriptionData);
            if (this.mQClientFactory != null) {
                this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
            }
        } catch (Exception e) {
            throw new MQClientException("subscription exception", e);
        }
    }

    public void subscribe(String topic, String fullClassName, String filterClassSource) throws MQClientException {
        try {
            SubscriptionData subscriptionData = FilterAPI.buildSubscriptionData(this.defaultMQPushConsumer.getConsumerGroup(), topic, "*");
            subscriptionData.setSubString(fullClassName);
            subscriptionData.setClassFilterMode(true);
            subscriptionData.setFilterClassSource(filterClassSource);
            this.rebalanceImpl.getSubscriptionInner().put(topic, subscriptionData);
            if (this.mQClientFactory != null) {
                this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
            }
        } catch (Exception e) {
            throw new MQClientException("subscription exception", e);
        }
    }

    public void subscribe(String topic, MessageSelector messageSelector) throws MQClientException {
        try {
            if (messageSelector == null) {
                this.subscribe(topic, "*");
            } else {
                SubscriptionData subscriptionData = FilterAPI.build(topic, messageSelector.getExpression(), messageSelector.getExpressionType());
                this.rebalanceImpl.getSubscriptionInner().put(topic, subscriptionData);
                if (this.mQClientFactory != null) {
                    this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
                }

            }
        } catch (Exception var4) {
            throw new MQClientException("subscription exception", var4);
        }
    }

    public void suspend() {
        this.pause = true;
        this.log.info("suspend this consumer, {}", this.defaultMQPushConsumer.getConsumerGroup());
    }

    public void unsubscribe(String topic) {
        this.rebalanceImpl.getSubscriptionInner().remove(topic);
    }

    public void updateConsumeOffset(MessageQueue mq, long offset) {
        this.offsetStore.updateOffset(mq, offset, false);
    }

    public void updateCorePoolSize(int corePoolSize) {
        this.consumeMessageService.updateCorePoolSize(corePoolSize);
    }

    public MessageExt viewMessage(String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return this.mQClientFactory.getMQAdminImpl().viewMessage(msgId);
    }

    public RebalanceImpl getRebalanceImpl() {
        return this.rebalanceImpl;
    }

    public boolean isConsumeOrderly() {
        return this.consumeOrderly;
    }

    public void setConsumeOrderly(boolean consumeOrderly) {
        this.consumeOrderly = consumeOrderly;
    }

    public void resetOffsetByTimeStamp(long timeStamp) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        Iterator var3 = this.rebalanceImpl.getSubscriptionInner().keySet().iterator();

        while(true) {
            String topic;
            Set mqs;
            HashMap offsetTable;
            do {
                if (!var3.hasNext()) {
                    return;
                }

                topic = (String)var3.next();
                mqs = (Set)this.rebalanceImpl.getTopicSubscribeInfoTable().get(topic);
                offsetTable = new HashMap();
            } while(mqs == null);

            Iterator var7 = mqs.iterator();

            while(var7.hasNext()) {
                MessageQueue mq = (MessageQueue)var7.next();
                long offset = this.searchOffset(mq, timeStamp);
                offsetTable.put(mq, offset);
            }

            this.mQClientFactory.resetOffset(topic, this.groupName(), offsetTable);
        }
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return this.mQClientFactory.getMQAdminImpl().searchOffset(mq, timestamp);
    }

    public String groupName() {
        return this.defaultMQPushConsumer.getConsumerGroup();
    }

    public MessageModel messageModel() {
        return this.defaultMQPushConsumer.getMessageModel();
    }

    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_PASSIVELY;
    }

    public ConsumeFromWhere consumeFromWhere() {
        return this.defaultMQPushConsumer.getConsumeFromWhere();
    }

    public Set<SubscriptionData> subscriptions() {
        Set<SubscriptionData> subSet = new HashSet();
        subSet.addAll(this.rebalanceImpl.getSubscriptionInner().values());
        return subSet;
    }

    public void doRebalance() {
        if (!this.pause) {
            this.rebalanceImpl.doRebalance(this.isConsumeOrderly());
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
            this.log.error("group: " + this.defaultMQPushConsumer.getConsumerGroup() + " persistConsumerOffset exception", var3);
        }

    }

    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {
        Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
        if (subTable != null && subTable.containsKey(topic)) {
            this.rebalanceImpl.topicSubscribeInfoTable.put(topic, info);
        }

    }

    public boolean isSubscribeTopicNeedUpdate(String topic) {
        Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
        if (subTable != null && subTable.containsKey(topic)) {
            return !this.rebalanceImpl.topicSubscribeInfoTable.containsKey(topic);
        } else {
            return false;
        }
    }

    public boolean isUnitMode() {
        return this.defaultMQPushConsumer.isUnitMode();
    }

    public ConsumerRunningInfo consumerRunningInfo() {
        ConsumerRunningInfo info = new ConsumerRunningInfo();
        Properties prop = MixAll.object2Properties(this.defaultMQPushConsumer);
        prop.put("PROP_CONSUMEORDERLY", String.valueOf(this.consumeOrderly));
        prop.put("PROP_THREADPOOL_CORE_SIZE", String.valueOf(this.consumeMessageService.getCorePoolSize()));
        prop.put("PROP_CONSUMER_START_TIMESTAMP", String.valueOf(this.consumerStartTimestamp));
        info.setProperties(prop);
        Set<SubscriptionData> subSet = this.subscriptions();
        info.getSubscriptionSet().addAll(subSet);
        Iterator<Map.Entry<MessageQueue, ProcessQueue>> it = this.rebalanceImpl.getProcessQueueTable().entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<MessageQueue, ProcessQueue> next = (Map.Entry)it.next();
            MessageQueue mq = (MessageQueue)next.getKey();
            ProcessQueue pq = (ProcessQueue)next.getValue();
            ProcessQueueInfo pqinfo = new ProcessQueueInfo();
            pqinfo.setCommitOffset(this.offsetStore.readOffset(mq, ReadOffsetType.MEMORY_FIRST_THEN_STORE));
            pq.fillProcessQueueInfo(pqinfo);
            info.getMqTable().put(mq, pqinfo);
        }

        Iterator var9 = subSet.iterator();

        while(var9.hasNext()) {
            SubscriptionData sd = (SubscriptionData)var9.next();
            ConsumeStatus consumeStatus = this.mQClientFactory.getConsumerStatsManager().consumeStatus(this.groupName(), sd.getTopic());
            info.getStatusTable().put(sd.getTopic(), consumeStatus);
        }

        return info;
    }

    public MQClientInstance getmQClientFactory() {
        return this.mQClientFactory;
    }

    public void setmQClientFactory(MQClientInstance mQClientFactory) {
        this.mQClientFactory = mQClientFactory;
    }

    public ServiceState getServiceState() {
        return this.serviceState;
    }

    /** @deprecated */
    @Deprecated
    public synchronized void setServiceState(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public void adjustThreadPool() {
        long computeAccTotal = this.computeAccumulationTotal();
        long adjustThreadPoolNumsThreshold = this.defaultMQPushConsumer.getAdjustThreadPoolNumsThreshold();
        long incThreshold = (long)((double)adjustThreadPoolNumsThreshold * 1.0);
        long decThreshold = (long)((double)adjustThreadPoolNumsThreshold * 0.8);
        if (computeAccTotal >= incThreshold) {
            this.consumeMessageService.incCorePoolSize();
        }

        if (computeAccTotal < decThreshold) {
            this.consumeMessageService.decCorePoolSize();
        }

    }

    private long computeAccumulationTotal() {
        long msgAccTotal = 0L;
        ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = this.rebalanceImpl.getProcessQueueTable();

        ProcessQueue value;
        for(Iterator<Map.Entry<MessageQueue, ProcessQueue>> it = processQueueTable.entrySet().iterator(); it.hasNext(); msgAccTotal += value.getMsgAccCnt()) {
            Map.Entry<MessageQueue, ProcessQueue> next = (Map.Entry)it.next();
            value = (ProcessQueue)next.getValue();
        }

        return msgAccTotal;
    }

    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        List<QueueTimeSpan> queueTimeSpan = new ArrayList();
        TopicRouteData routeData = this.mQClientFactory.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(topic, 3000L);
        Iterator var4 = routeData.getBrokerDatas().iterator();

        while(var4.hasNext()) {
            BrokerData brokerData = (BrokerData)var4.next();
            String addr = brokerData.selectBrokerAddr();
            queueTimeSpan.addAll(this.mQClientFactory.getMQClientAPIImpl().queryConsumeTimeSpan(addr, topic, this.groupName(), 3000L));
        }

        return queueTimeSpan;
    }

    public void resetRetryAndNamespace(List<MessageExt> msgs, String consumerGroup) {
        String groupTopic = MixAll.getRetryTopic(consumerGroup);
        Iterator var4 = msgs.iterator();

        while(var4.hasNext()) {
            MessageExt msg = (MessageExt)var4.next();
            String retryTopic = msg.getProperty("RETRY_TOPIC");
            if (retryTopic != null && groupTopic.equals(msg.getTopic())) {
                msg.setTopic(retryTopic);
            }

            if (StringUtils.isNotEmpty(this.defaultMQPushConsumer.getNamespace())) {
                msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQPushConsumer.getNamespace()));
            }
        }

    }

    public ConsumeMessageService getConsumeMessageService() {
        return this.consumeMessageService;
    }

    public void setConsumeMessageService(ConsumeMessageService consumeMessageService) {
        this.consumeMessageService = consumeMessageService;
    }
}
