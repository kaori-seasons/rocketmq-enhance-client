package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.client.impl.FindBrokerResult;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.LockBatchRequestBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.UnlockBatchRequestBody;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class RebalanceImpl {
    protected static final InternalLogger log = ClientLogger.getLog();
    protected final ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = new ConcurrentHashMap(64);
    protected final ConcurrentMap<String, Set<MessageQueue>> topicSubscribeInfoTable = new ConcurrentHashMap();
    protected final ConcurrentMap<String, SubscriptionData> subscriptionInner = new ConcurrentHashMap();
    protected String consumerGroup;
    protected MessageModel messageModel;
    protected AllocateMessageQueueStrategy allocateMessageQueueStrategy;
    protected MQClientInstance mQClientFactory;

    public abstract void messageQueueChanged(String str, Set<MessageQueue> set, Set<MessageQueue> set2);

    public abstract boolean removeUnnecessaryMessageQueue(MessageQueue messageQueue, ProcessQueue processQueue);

    public abstract ConsumeType consumeType();

    public abstract void removeDirtyOffset(MessageQueue messageQueue);

    public abstract long computePullFromWhere(MessageQueue messageQueue);

    public abstract void dispatchPullRequest(List<PullRequest> list);

    public RebalanceImpl(String consumerGroup, MessageModel messageModel, AllocateMessageQueueStrategy allocateMessageQueueStrategy, MQClientInstance mQClientFactory) {
        this.consumerGroup = consumerGroup;
        this.messageModel = messageModel;
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
        this.mQClientFactory = mQClientFactory;
    }

    public void unlock(MessageQueue mq, boolean oneway) {
        FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), 0, true);
        if (findBrokerResult != null) {
            UnlockBatchRequestBody requestBody = new UnlockBatchRequestBody();
            requestBody.setConsumerGroup(this.consumerGroup);
            requestBody.setClientId(this.mQClientFactory.getClientId());
            requestBody.getMqSet().add(mq);
            try {
                this.mQClientFactory.getMQClientAPIImpl().unlockBatchMQ(findBrokerResult.getBrokerAddr(), requestBody, 1000, oneway);
                log.warn("unlock messageQueue. group:{}, clientId:{}, mq:{}", this.consumerGroup, this.mQClientFactory.getClientId(), mq);
            } catch (Exception e) {
                log.error("unlockBatchMQ exception, " + mq, (Throwable) e);
            }
        }
    }

    public void unlockAll(boolean oneway) {
        FindBrokerResult findBrokerResult;
        for (Map.Entry<String, Set<MessageQueue>> entry : buildProcessQueueTableByBrokerName().entrySet()) {
            String brokerName = entry.getKey();
            Set<MessageQueue> mqs = entry.getValue();
            if (!mqs.isEmpty() && (findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(brokerName, 0, true)) != null) {
                UnlockBatchRequestBody requestBody = new UnlockBatchRequestBody();
                requestBody.setConsumerGroup(this.consumerGroup);
                requestBody.setClientId(this.mQClientFactory.getClientId());
                requestBody.setMqSet(mqs);
                try {
                    this.mQClientFactory.getMQClientAPIImpl().unlockBatchMQ(findBrokerResult.getBrokerAddr(), requestBody, 1000, oneway);
                    for (MessageQueue mq : mqs) {
                        ProcessQueue processQueue = this.processQueueTable.get(mq);
                        if (processQueue != null) {
                            processQueue.setLocked(false);
                            log.info("the message queue unlock OK, Group: {} {}", this.consumerGroup, mq);
                        }
                    }
                } catch (Exception e) {
                    log.error("unlockBatchMQ exception, " + mqs, (Throwable) e);
                }
            }
        }
    }

    private HashMap<String, Set<MessageQueue>> buildProcessQueueTableByBrokerName() {
        HashMap<String, Set<MessageQueue>> result = new HashMap<>();
        for (MessageQueue mq : this.processQueueTable.keySet()) {
            Set<MessageQueue> mqs = result.get(mq.getBrokerName());
            if (null == mqs) {
                mqs = new HashSet();
                result.put(mq.getBrokerName(), mqs);
            }
            mqs.add(mq);
        }
        return result;
    }

    public boolean lock(MessageQueue mq) {
        FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), 0, true);
        if (findBrokerResult == null) {
            return false;
        }
        LockBatchRequestBody requestBody = new LockBatchRequestBody();
        requestBody.setConsumerGroup(this.consumerGroup);
        requestBody.setClientId(this.mQClientFactory.getClientId());
        requestBody.getMqSet().add(mq);
        try {
            Set<MessageQueue> lockedMq = this.mQClientFactory.getMQClientAPIImpl().lockBatchMQ(findBrokerResult.getBrokerAddr(), requestBody, 1000);
            for (MessageQueue mmqq : lockedMq) {
                ProcessQueue processQueue = this.processQueueTable.get(mmqq);
                if (processQueue != null) {
                    processQueue.setLocked(true);
                    processQueue.setLastLockTimestamp(System.currentTimeMillis());
                }
            }
            boolean lockOK = lockedMq.contains(mq);
            InternalLogger internalLogger = log;
            Object[] objArr = new Object[3];
            objArr[0] = lockOK ? "OK" : "Failed";
            objArr[1] = this.consumerGroup;
            objArr[2] = mq;
            internalLogger.info("the message queue lock {}, {} {}", objArr);
            return lockOK;
        } catch (Exception e) {
            log.error("lockBatchMQ exception, " + mq, (Throwable) e);
            return false;
        }
    }

    public void lockAll() {
        FindBrokerResult findBrokerResult;
        ProcessQueue processQueue;
        for (Map.Entry<String, Set<MessageQueue>> entry : buildProcessQueueTableByBrokerName().entrySet()) {
            String brokerName = entry.getKey();
            Set<MessageQueue> mqs = entry.getValue();
            if (!mqs.isEmpty() && (findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(brokerName, 0, true)) != null) {
                LockBatchRequestBody requestBody = new LockBatchRequestBody();
                requestBody.setConsumerGroup(this.consumerGroup);
                requestBody.setClientId(this.mQClientFactory.getClientId());
                requestBody.setMqSet(mqs);
                try {
                    Set<MessageQueue> lockOKMQSet = this.mQClientFactory.getMQClientAPIImpl().lockBatchMQ(findBrokerResult.getBrokerAddr(), requestBody, 1000);
                    for (MessageQueue mq : lockOKMQSet) {
                        ProcessQueue processQueue2 = this.processQueueTable.get(mq);
                        if (processQueue2 != null) {
                            if (!processQueue2.isLocked()) {
                                log.info("the message queue locked OK, Group: {} {}", this.consumerGroup, mq);
                            }
                            processQueue2.setLocked(true);
                            processQueue2.setLastLockTimestamp(System.currentTimeMillis());
                        }
                    }
                    for (MessageQueue mq2 : mqs) {
                        if (!lockOKMQSet.contains(mq2) && (processQueue = this.processQueueTable.get(mq2)) != null) {
                            processQueue.setLocked(false);
                            log.warn("the message queue locked Failed, Group: {} {}", this.consumerGroup, mq2);
                        }
                    }
                } catch (Exception e) {
                    log.error("lockBatchMQ exception, " + mqs, (Throwable) e);
                }
            }
        }
    }

    public void doRebalance(boolean isOrder) {
        Map<String, SubscriptionData> subTable = getSubscriptionInner();
        if (subTable != null) {
            for (Map.Entry<String, SubscriptionData> entry : subTable.entrySet()) {
                String topic = entry.getKey();
                try {
                    rebalanceByTopic(topic, isOrder);
                } catch (Throwable e) {
                    if (!topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                        log.warn("rebalanceByTopic Exception", e);
                    }
                }
            }
        }
        truncateMessageQueueNotMyTopic();
    }

    public ConcurrentMap<String, SubscriptionData> getSubscriptionInner() {
        return this.subscriptionInner;
    }

    private void rebalanceByTopic(String topic, boolean isOrder) {
        switch (this.messageModel) {
            case BROADCASTING:
                Set<MessageQueue> mqSet = this.topicSubscribeInfoTable.get(topic);
                if (mqSet == null) {
                    log.warn("doRebalance, {}, but the topic[{}] not exist.", this.consumerGroup, topic);
                    return;
                } else if (updateProcessQueueTableInRebalance(topic, mqSet, isOrder)) {
                    messageQueueChanged(topic, mqSet, mqSet);
                    log.info("messageQueueChanged {} {} {} {}", this.consumerGroup, topic, mqSet, mqSet);
                    return;
                } else {
                    return;
                }
            case CLUSTERING:
                Set<MessageQueue> mqSet2 = this.topicSubscribeInfoTable.get(topic);
                List<String> cidAll = this.mQClientFactory.findConsumerIdList(topic, this.consumerGroup);
                if (null == mqSet2 && !topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    log.warn("doRebalance, {}, but the topic[{}] not exist.", this.consumerGroup, topic);
                }
                if (null == cidAll) {
                    log.warn("doRebalance, {} {}, get consumer id list failed", this.consumerGroup, topic);
                }
                if (mqSet2 != null && cidAll != null) {
                    List<MessageQueue> mqAll = new ArrayList<>();
                    mqAll.addAll(mqSet2);
                    Collections.sort(mqAll);
                    Collections.sort(cidAll);
                    AllocateMessageQueueStrategy strategy = this.allocateMessageQueueStrategy;
                    try {
                        Collection<? extends MessageQueue> allocateResult = strategy.allocate(this.consumerGroup, this.mQClientFactory.getClientId(), mqAll, cidAll);
                        Set<MessageQueue> allocateResultSet = new HashSet<>();
                        if (allocateResult != null) {
                            allocateResultSet.addAll(allocateResult);
                        }
                        if (updateProcessQueueTableInRebalance(topic, allocateResultSet, isOrder)) {
                            log.info("rebalanced result changed. allocateMessageQueueStrategyName={}, group={}, topic={}, clientId={}, mqAllSize={}, cidAllSize={}, rebalanceResultSize={}, rebalanceResultSet={}", strategy.getName(), this.consumerGroup, topic, this.mQClientFactory.getClientId(), Integer.valueOf(mqSet2.size()), Integer.valueOf(cidAll.size()), Integer.valueOf(allocateResultSet.size()), allocateResultSet);
                            messageQueueChanged(topic, mqSet2, allocateResultSet);
                            return;
                        }
                        return;
                    } catch (Throwable e) {
                        log.error("AllocateMessageQueueStrategy.allocate Exception. allocateMessageQueueStrategyName={}", strategy.getName(), e);
                        return;
                    }
                } else {
                    return;
                }
            default:
                return;
        }
    }

    private void truncateMessageQueueNotMyTopic() {
        ProcessQueue pq;
        Map<String, SubscriptionData> subTable = getSubscriptionInner();
        for (MessageQueue mq : this.processQueueTable.keySet()) {
            if (!subTable.containsKey(mq.getTopic()) && (pq = this.processQueueTable.remove(mq)) != null) {
                pq.setDropped(true);
                log.info("doRebalance, {}, truncateMessageQueueNotMyTopic remove unnecessary mq, {}", this.consumerGroup, mq);
            }
        }
    }

    private boolean updateProcessQueueTableInRebalance(String topic, Set<MessageQueue> mqSet, boolean isOrder) {
        boolean changed = false;
        Iterator<Map.Entry<MessageQueue, ProcessQueue>> it = this.processQueueTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MessageQueue, ProcessQueue> next = it.next();
            MessageQueue mq = next.getKey();
            ProcessQueue pq = next.getValue();
            if (mq.getTopic().equals(topic)) {
                if (!mqSet.contains(mq)) {
                    pq.setDropped(true);
                    if (removeUnnecessaryMessageQueue(mq, pq)) {
                        it.remove();
                        changed = true;
                        log.info("doRebalance, {}, remove unnecessary mq, {}", this.consumerGroup, mq);
                    }
                } else if (pq.isPullExpired()) {
                    switch (consumeType()) {
                        case CONSUME_PASSIVELY:
                            pq.setDropped(true);
                            if (removeUnnecessaryMessageQueue(mq, pq)) {
                                it.remove();
                                changed = true;
                                log.error("[BUG]doRebalance, {}, remove unnecessary mq, {}, because pull is pause, so try to fixed it", this.consumerGroup, mq);
                                break;
                            } else {
                                continue;
                            }
                    }
                }
            }
        }
        List<PullRequest> pullRequestList = new ArrayList<>();
        for (MessageQueue mq2 : mqSet) {
            if (!this.processQueueTable.containsKey(mq2)) {
                if (!isOrder || lock(mq2)) {
                    removeDirtyOffset(mq2);
                    ProcessQueue pq2 = new ProcessQueue();
                    long nextOffset = computePullFromWhere(mq2);
                    if (nextOffset < 0) {
                        log.warn("doRebalance, {}, add new mq failed, {}", this.consumerGroup, mq2);
                    } else if (this.processQueueTable.putIfAbsent(mq2, pq2) != null) {
                        log.info("doRebalance, {}, mq already exists, {}", this.consumerGroup, mq2);
                    } else {
                        log.info("doRebalance, {}, add a new mq, {}", this.consumerGroup, mq2);
                        PullRequest pullRequest = new PullRequest();
                        pullRequest.setConsumerGroup(this.consumerGroup);
                        pullRequest.setNextOffset(nextOffset);
                        pullRequest.setMessageQueue(mq2);
                        pullRequest.setProcessQueue(pq2);
                        pullRequestList.add(pullRequest);
                        changed = true;
                    }
                } else {
                    log.warn("doRebalance, {}, add a new mq failed, {}, because lock failed", this.consumerGroup, mq2);
                }
            }
        }
        dispatchPullRequest(pullRequestList);
        return changed;
    }

    public void removeProcessQueue(MessageQueue mq) {
        ProcessQueue prev = this.processQueueTable.remove(mq);
        if (prev != null) {
            boolean droped = prev.isDropped();
            prev.setDropped(true);
            removeUnnecessaryMessageQueue(mq, prev);
            log.info("Fix Offset, {}, remove unnecessary mq, {} Droped: {}", this.consumerGroup, mq, Boolean.valueOf(droped));
        }
    }

    public ConcurrentMap<MessageQueue, ProcessQueue> getProcessQueueTable() {
        return this.processQueueTable;
    }

    public ConcurrentMap<String, Set<MessageQueue>> getTopicSubscribeInfoTable() {
        return this.topicSubscribeInfoTable;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public MessageModel getMessageModel() {
        return this.messageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public AllocateMessageQueueStrategy getAllocateMessageQueueStrategy() {
        return this.allocateMessageQueueStrategy;
    }

    public void setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy allocateMessageQueueStrategy) {
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
    }

    public MQClientInstance getmQClientFactory() {
        return this.mQClientFactory;
    }

    public void setmQClientFactory(MQClientInstance mQClientFactory) {
        this.mQClientFactory = mQClientFactory;
    }

    public void destroy() {
        for (Map.Entry<MessageQueue, ProcessQueue> next : this.processQueueTable.entrySet()) {
            next.getValue().setDropped(true);
        }
        this.processQueueTable.clear();
    }
}
