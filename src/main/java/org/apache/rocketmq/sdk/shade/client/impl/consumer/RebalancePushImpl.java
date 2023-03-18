package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.client.consumer.store.OffsetStore;
import org.apache.rocketmq.sdk.shade.client.consumer.store.ReadOffsetType;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RebalancePushImpl extends RebalanceImpl {
    private static final long UNLOCK_DELAY_TIME_MILLS = Long.parseLong(System.getProperty("rocketmq.client.unlockDelayTimeMills", "20000"));
    private final DefaultMQPushConsumerImpl defaultMQPushConsumerImpl;

    public RebalancePushImpl(DefaultMQPushConsumerImpl defaultMQPushConsumerImpl) {
        this(null, null, null, null, defaultMQPushConsumerImpl);
    }

    public RebalancePushImpl(String consumerGroup, MessageModel messageModel, AllocateMessageQueueStrategy allocateMessageQueueStrategy, MQClientInstance mQClientFactory, DefaultMQPushConsumerImpl defaultMQPushConsumerImpl) {
        super(consumerGroup, messageModel, allocateMessageQueueStrategy, mQClientFactory);
        this.defaultMQPushConsumerImpl = defaultMQPushConsumerImpl;
    }

    @Override 
    public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {
        SubscriptionData subscriptionData = (SubscriptionData) this.subscriptionInner.get(topic);
        long newVersion = System.currentTimeMillis();
        log.info("{} Rebalance changed, also update version: {}, {}", topic, Long.valueOf(subscriptionData.getSubVersion()), Long.valueOf(newVersion));
        subscriptionData.setSubVersion(newVersion);
        int currentQueueCount = this.processQueueTable.size();
        if (currentQueueCount != 0) {
            int pullThresholdForTopic = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdForTopic();
            if (pullThresholdForTopic != -1) {
                int newVal = Math.max(1, pullThresholdForTopic / currentQueueCount);
                log.info("The pullThresholdForQueue is changed from {} to {}", Integer.valueOf(this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdForQueue()), Integer.valueOf(newVal));
                this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().setPullThresholdForQueue(newVal);
            }
            int pullThresholdSizeForTopic = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdSizeForTopic();
            if (pullThresholdSizeForTopic != -1) {
                int newVal2 = Math.max(1, pullThresholdSizeForTopic / currentQueueCount);
                log.info("The pullThresholdSizeForQueue is changed from {} to {}", Integer.valueOf(this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdSizeForQueue()), Integer.valueOf(newVal2));
                this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().setPullThresholdSizeForQueue(newVal2);
            }
        }
        getmQClientFactory().sendHeartbeatToAllBrokerWithTimedLock();
    }

    @Override 
    public boolean removeUnnecessaryMessageQueue(MessageQueue mq, ProcessQueue pq) {
        this.defaultMQPushConsumerImpl.getOffsetStore().persist(mq);
        this.defaultMQPushConsumerImpl.getOffsetStore().removeOffset(mq);
        if (!this.defaultMQPushConsumerImpl.isConsumeOrderly() || !MessageModel.CLUSTERING.equals(this.defaultMQPushConsumerImpl.messageModel())) {
            return true;
        }
        try {
            if (pq.getLockConsume().tryLock(1000, TimeUnit.MILLISECONDS)) {
                boolean unlockDelay = unlockDelay(mq, pq);
                pq.getLockConsume().unlock();
                return unlockDelay;
            }
            log.warn("[WRONG]mq is consuming, so can not unlock it, {}. maybe hanged for a while, {}", mq, Long.valueOf(pq.getTryUnlockTimes()));
            pq.incTryUnlockTimes();
            return false;
        } catch (Exception e) {
            log.error("removeUnnecessaryMessageQueue Exception", (Throwable) e);
            return false;
        }
    }

    private boolean unlockDelay(final MessageQueue mq, ProcessQueue pq) {
        if (pq.hasTempMessage()) {
            log.info("[{}]unlockDelay, begin {} ", Integer.valueOf(mq.hashCode()), mq);
            this.defaultMQPushConsumerImpl.getmQClientFactory().getScheduledExecutorService().schedule(new Runnable() {
                @Override
                public void run() {
                    RebalanceImpl.log.info("[{}]unlockDelay, execute at once {}", Integer.valueOf(mq.hashCode()), mq);
                    RebalancePushImpl.this.unlock(mq, true);
                }
            }, UNLOCK_DELAY_TIME_MILLS, TimeUnit.MILLISECONDS);
            return true;
        }
        unlock(mq, true);
        return true;
    }

    @Override 
    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_PASSIVELY;
    }

    @Override 
    public void removeDirtyOffset(MessageQueue mq) {
        this.defaultMQPushConsumerImpl.getOffsetStore().removeOffset(mq);
    }


    @Override
    public long computePullFromWhere(MessageQueue mq) {
        long result = -1L;
        ConsumeFromWhere consumeFromWhere = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getConsumeFromWhere();
        OffsetStore offsetStore = this.defaultMQPushConsumerImpl.getOffsetStore();
        long lastOffset;
        switch(consumeFromWhere) {
            case CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST:
            case CONSUME_FROM_MIN_OFFSET:
            case CONSUME_FROM_MAX_OFFSET:
            case CONSUME_FROM_LAST_OFFSET:
                lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0L) {
                    result = lastOffset;
                } else if (-1L == lastOffset) {
                    if (mq.getTopic().startsWith("%RETRY%")) {
                        result = 0L;
                    } else {
                        try {
                            result = this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
                        } catch (MQClientException var13) {
                            result = -1L;
                        }
                    }
                } else {
                    result = -1L;
                }
                break;
            case CONSUME_FROM_FIRST_OFFSET:
                lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0L) {
                    result = lastOffset;
                } else if (-1L == lastOffset) {
                    result = 0L;
                } else {
                    result = -1L;
                }
                break;
            case CONSUME_FROM_TIMESTAMP:
                lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0L) {
                    result = lastOffset;
                } else if (-1L == lastOffset) {
                    if (mq.getTopic().startsWith("%RETRY%")) {
                        try {
                            result = this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
                        } catch (MQClientException var12) {
                            result = -1L;
                        }
                    } else {
                        try {
                            Date time = UtilAll.parseDate(this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getConsumeTimestamp(), "yyyyMMddHHmmss");
                            if (time != null) {
                                long timestamp = time.getTime();
                                result = this.mQClientFactory.getMQAdminImpl().searchOffset(mq, timestamp);
                            }
                        } catch (MQClientException var11) {
                            result = -1L;
                        }
                    }
                } else {
                    result = -1L;
                }
        }

        return result;
    }

    @Override 
    public void dispatchPullRequest(List<PullRequest> pullRequestList) {
        for (PullRequest pullRequest : pullRequestList) {
            this.defaultMQPushConsumerImpl.executePullRequestImmediately(pullRequest);
            log.info("doRebalance, {}, add a new pull request {}", this.consumerGroup, pullRequest);
        }
    }
}
