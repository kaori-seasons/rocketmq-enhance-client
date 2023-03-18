package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.ThreadFactoryImpl;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.CMResult;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeReturnType;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class ConsumeMessageOrderlyService implements ConsumeMessageService {
    private static final InternalLogger log = ClientLogger.getLog();
    private static final long MAX_TIME_CONSUME_CONTINUOUSLY = Long.parseLong(System.getProperty("rocketmq.client.maxTimeConsumeContinuously", "60000"));
    private final DefaultMQPushConsumerImpl defaultMQPushConsumerImpl;
    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final MessageListenerOrderly messageListener;
    private final BlockingQueue<Runnable> consumeRequestQueue;
    private final ThreadPoolExecutor consumeExecutor;
    private final String consumerGroup;
    private final MessageQueueLock messageQueueLock = new MessageQueueLock();
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile boolean stopped = false;

    public ConsumeMessageOrderlyService(DefaultMQPushConsumerImpl defaultMQPushConsumerImpl, MessageListenerOrderly messageListener) {
        this.defaultMQPushConsumerImpl = defaultMQPushConsumerImpl;
        this.messageListener = messageListener;
        this.defaultMQPushConsumer = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer();
        this.consumerGroup = this.defaultMQPushConsumer.getConsumerGroup();
        this.consumeRequestQueue = new LinkedBlockingQueue();
        this.consumeExecutor = new ThreadPoolExecutor(this.defaultMQPushConsumer.getConsumeThreadMin(), this.defaultMQPushConsumer.getConsumeThreadMax(), 60000L, TimeUnit.MILLISECONDS, this.consumeRequestQueue, new ThreadFactoryImpl("ConsumeMessageThread_"));
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ConsumeMessageScheduledThread_"));
    }

    public void start() {
        if (MessageModel.CLUSTERING.equals(this.defaultMQPushConsumerImpl.messageModel())) {
            this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    ConsumeMessageOrderlyService.this.lockMQPeriodically();
                }
            }, 1000L, ProcessQueue.REBALANCE_LOCK_INTERVAL, TimeUnit.MILLISECONDS);
        }

    }

    public void shutdown() {
        this.stopped = true;
        this.scheduledExecutorService.shutdown();
        this.consumeExecutor.shutdown();
        if (MessageModel.CLUSTERING.equals(this.defaultMQPushConsumerImpl.messageModel())) {
            this.unlockAllMQ();
        }

    }

    public synchronized void unlockAllMQ() {
        this.defaultMQPushConsumerImpl.getRebalanceImpl().unlockAll(false);
    }

    public void updateCorePoolSize(int corePoolSize) {
        if (corePoolSize > 0 && corePoolSize <= 32767 && corePoolSize < this.defaultMQPushConsumer.getConsumeThreadMax()) {
            this.consumeExecutor.setCorePoolSize(corePoolSize);
        }

    }

    public void incCorePoolSize() {
    }

    public void decCorePoolSize() {
    }

    public int getCorePoolSize() {
        return this.consumeExecutor.getCorePoolSize();
    }

    public ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt msg, String brokerName) {
        ConsumeMessageDirectlyResult result = new ConsumeMessageDirectlyResult();
        result.setOrder(true);
        List<MessageExt> msgs = new ArrayList();
        msgs.add(msg);
        MessageQueue mq = new MessageQueue();
        mq.setBrokerName(brokerName);
        mq.setTopic(msg.getTopic());
        mq.setQueueId(msg.getQueueId());
        ConsumeOrderlyContext context = new ConsumeOrderlyContext(mq);
        this.defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, this.consumerGroup);
        long beginTime = System.currentTimeMillis();
        log.info("consumeMessageDirectly receive new message: {}", msg);

        try {
            ConsumeOrderlyStatus status = this.messageListener.consumeMessage(msgs, context);
            if (status != null) {
                switch(status) {
                    case COMMIT:
                        result.setConsumeResult(CMResult.CR_COMMIT);
                        break;
                    case ROLLBACK:
                        result.setConsumeResult(CMResult.CR_ROLLBACK);
                        break;
                    case SUCCESS:
                        result.setConsumeResult(CMResult.CR_SUCCESS);
                        break;
                    case SUSPEND_CURRENT_QUEUE_A_MOMENT:
                        result.setConsumeResult(CMResult.CR_LATER);
                }
            } else {
                result.setConsumeResult(CMResult.CR_RETURN_NULL);
            }
        } catch (Throwable var10) {
            result.setConsumeResult(CMResult.CR_THROW_EXCEPTION);
            result.setRemark(RemotingHelper.exceptionSimpleDesc(var10));
            log.warn(String.format("consumeMessageDirectly exception: %s Group: %s Msgs: %s MQ: %s", RemotingHelper.exceptionSimpleDesc(var10), this.consumerGroup, msgs, mq), var10);
        }

        result.setAutoCommit(context.isAutoCommit());
        result.setSpentTimeMills(System.currentTimeMillis() - beginTime);
        log.info("consumeMessageDirectly Result: {}", result);
        return result;
    }

    public void submitConsumeRequest(List<MessageExt> msgs, ProcessQueue processQueue, MessageQueue messageQueue, boolean dispathToConsume) {
        if (dispathToConsume) {
            ConsumeMessageOrderlyService.ConsumeRequest consumeRequest = new ConsumeMessageOrderlyService.ConsumeRequest(processQueue, messageQueue);
            this.consumeExecutor.submit(consumeRequest);
        }

    }

    public synchronized void lockMQPeriodically() {
        if (!this.stopped) {
            this.defaultMQPushConsumerImpl.getRebalanceImpl().lockAll();
        }

    }

    public void tryLockLaterAndReconsume(final MessageQueue mq, final ProcessQueue processQueue, long delayMills) {
        this.scheduledExecutorService.schedule(new Runnable() {
            public void run() {
                boolean lockOK = ConsumeMessageOrderlyService.this.lockOneMQ(mq);
                if (lockOK) {
                    ConsumeMessageOrderlyService.this.submitConsumeRequestLater(processQueue, mq, 10L);
                } else {
                    ConsumeMessageOrderlyService.this.submitConsumeRequestLater(processQueue, mq, 3000L);
                }

            }
        }, delayMills, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean lockOneMQ(MessageQueue mq) {
        return !this.stopped ? this.defaultMQPushConsumerImpl.getRebalanceImpl().lock(mq) : false;
    }

    private void submitConsumeRequestLater(final ProcessQueue processQueue, final MessageQueue messageQueue, long suspendTimeMillis) {
        long timeMillis = suspendTimeMillis;
        if (suspendTimeMillis == -1L) {
            timeMillis = this.defaultMQPushConsumer.getSuspendCurrentQueueTimeMillis();
        }

        if (timeMillis < 10L) {
            timeMillis = 10L;
        } else if (timeMillis > 30000L) {
            timeMillis = 30000L;
        }

        this.scheduledExecutorService.schedule(new Runnable() {
            public void run() {
                ConsumeMessageOrderlyService.this.submitConsumeRequest((List)null, processQueue, messageQueue, true);
            }
        }, timeMillis, TimeUnit.MILLISECONDS);
    }

    public boolean processConsumeResult(List<MessageExt> msgs, ConsumeOrderlyStatus status, ConsumeOrderlyContext context, ConsumeMessageOrderlyService.ConsumeRequest consumeRequest) {
        boolean continueConsume = true;
        long commitOffset = -1L;
        if (context.isAutoCommit()) {
            switch(status) {
                case COMMIT:
                case ROLLBACK:
                    log.warn("the message queue consume result is illegal, we think you want to ack these message {}", consumeRequest.getMessageQueue());
                    break;
                case SUCCESS:
                    commitOffset = consumeRequest.getProcessQueue().commit();
                    this.getConsumerStatsManager().incConsumeOKTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long)msgs.size());
                    break;
                case SUSPEND_CURRENT_QUEUE_A_MOMENT:
                    this.getConsumerStatsManager().incConsumeFailedTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long)msgs.size());
                    if (this.checkReconsumeTimes(msgs)) {
                        consumeRequest.getProcessQueue().makeMessageToCosumeAgain(msgs);
                        this.submitConsumeRequestLater(consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue(), context.getSuspendCurrentQueueTimeMillis());
                        continueConsume = false;
                    } else {
                        commitOffset = consumeRequest.getProcessQueue().commit();
                    }
            }
        } else {
            switch(status) {
                case COMMIT:
                    commitOffset = consumeRequest.getProcessQueue().commit();
                    break;
                case ROLLBACK:
                    consumeRequest.getProcessQueue().rollback();
                    this.submitConsumeRequestLater(consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue(), context.getSuspendCurrentQueueTimeMillis());
                    continueConsume = false;
                    break;
                case SUCCESS:
                    this.getConsumerStatsManager().incConsumeOKTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long)msgs.size());
                    break;
                case SUSPEND_CURRENT_QUEUE_A_MOMENT:
                    this.getConsumerStatsManager().incConsumeFailedTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long)msgs.size());
                    if (this.checkReconsumeTimes(msgs)) {
                        consumeRequest.getProcessQueue().makeMessageToCosumeAgain(msgs);
                        this.submitConsumeRequestLater(consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue(), context.getSuspendCurrentQueueTimeMillis());
                        continueConsume = false;
                    }
            }
        }

        if (commitOffset >= 0L && !consumeRequest.getProcessQueue().isDropped()) {
            this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
        }

        return continueConsume;
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return this.defaultMQPushConsumerImpl.getConsumerStatsManager();
    }

    private int getMaxReconsumeTimes() {
        return this.defaultMQPushConsumer.getMaxReconsumeTimes() == -1 ? 2147483647 : this.defaultMQPushConsumer.getMaxReconsumeTimes();
    }

    private boolean checkReconsumeTimes(List<MessageExt> msgs) {
        boolean suspend = false;
        if (msgs != null && !msgs.isEmpty()) {
            Iterator var3 = msgs.iterator();

            while(var3.hasNext()) {
                MessageExt msg = (MessageExt)var3.next();
                if (msg.getReconsumeTimes() >= this.getMaxReconsumeTimes()) {
                    MessageAccessor.setReconsumeTime(msg, String.valueOf(msg.getReconsumeTimes()));
                    if (!this.sendMessageBack(msg)) {
                        suspend = true;
                        msg.setReconsumeTimes(msg.getReconsumeTimes() + 1);
                    }
                } else {
                    suspend = true;
                    msg.setReconsumeTimes(msg.getReconsumeTimes() + 1);
                }
            }
        }

        return suspend;
    }

    public boolean sendMessageBack(MessageExt msg) {
        try {
            Message newMsg = new Message(MixAll.getRetryTopic(this.defaultMQPushConsumer.getConsumerGroup()), msg.getBody());
            String originMsgId = MessageAccessor.getOriginMessageId(msg);
            MessageAccessor.setOriginMessageId(newMsg, UtilAll.isBlank(originMsgId) ? msg.getMsgId() : originMsgId);
            newMsg.setFlag(msg.getFlag());
            MessageAccessor.setProperties(newMsg, msg.getProperties());
            MessageAccessor.putProperty(newMsg, "RETRY_TOPIC", msg.getTopic());
            MessageAccessor.setReconsumeTime(newMsg, String.valueOf(msg.getReconsumeTimes()));
            MessageAccessor.setMaxReconsumeTimes(newMsg, String.valueOf(this.getMaxReconsumeTimes()));
            newMsg.setDelayTimeLevel(3 + msg.getReconsumeTimes());
            this.defaultMQPushConsumer.getDefaultMQPushConsumerImpl().getmQClientFactory().getDefaultMQProducer().send(newMsg);
            return true;
        } catch (Exception var4) {
            log.error("sendMessageBack exception, group: " + this.consumerGroup + " msg: " + msg.toString(), var4);
            return false;
        }
    }

    class ConsumeRequest implements Runnable {
        private final ProcessQueue processQueue;
        private final MessageQueue messageQueue;

        public ConsumeRequest(ProcessQueue processQueue, MessageQueue messageQueue) {
            this.processQueue = processQueue;
            this.messageQueue = messageQueue;
        }

        public ProcessQueue getProcessQueue() {
            return this.processQueue;
        }

        public MessageQueue getMessageQueue() {
            return this.messageQueue;
        }

        public void run() {
            if (this.processQueue.isDropped()) {
                ConsumeMessageOrderlyService.log.warn("run, the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
            } else {
                Object objLock = ConsumeMessageOrderlyService.this.messageQueueLock.fetchLockObject(this.messageQueue);
                synchronized(objLock) {
                    if (!MessageModel.BROADCASTING.equals(ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.messageModel()) && (!this.processQueue.isLocked() || this.processQueue.isLockExpired())) {
                        if (this.processQueue.isDropped()) {
                            ConsumeMessageOrderlyService.log.warn("the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
                        } else {
                            ConsumeMessageOrderlyService.this.tryLockLaterAndReconsume(this.messageQueue, this.processQueue, 100L);
                        }
                    } else {
                        long beginTime = System.currentTimeMillis();
                        boolean continueConsume = true;

                        while(true) {
                            while(continueConsume) {
                                if (this.processQueue.isDropped()) {
                                    ConsumeMessageOrderlyService.log.warn("the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
                                    return;
                                }

                                if (MessageModel.CLUSTERING.equals(ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.messageModel()) && !this.processQueue.isLocked()) {
                                    ConsumeMessageOrderlyService.log.warn("the message queue not locked, so consume later, {}", this.messageQueue);
                                    ConsumeMessageOrderlyService.this.tryLockLaterAndReconsume(this.messageQueue, this.processQueue, 10L);
                                    return;
                                }

                                if (MessageModel.CLUSTERING.equals(ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.messageModel()) && this.processQueue.isLockExpired()) {
                                    ConsumeMessageOrderlyService.log.warn("the message queue lock expired, so consume later, {}", this.messageQueue);
                                    ConsumeMessageOrderlyService.this.tryLockLaterAndReconsume(this.messageQueue, this.processQueue, 10L);
                                    return;
                                }

                                long interval = System.currentTimeMillis() - beginTime;
                                if (interval > ConsumeMessageOrderlyService.MAX_TIME_CONSUME_CONTINUOUSLY) {
                                    ConsumeMessageOrderlyService.this.submitConsumeRequestLater(this.processQueue, this.messageQueue, 10L);
                                    return;
                                }

                                int consumeBatchSize = ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();
                                List<MessageExt> msgs = this.processQueue.takeMessags(consumeBatchSize);
                                ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getConsumerGroup());
                                if (!msgs.isEmpty()) {
                                    ConsumeOrderlyContext context = new ConsumeOrderlyContext(this.messageQueue);
                                    ConsumeOrderlyStatus status = null;
                                    ConsumeMessageContext consumeMessageContext = null;
                                    if (ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                                        consumeMessageContext = new ConsumeMessageContext();
                                        consumeMessageContext.setConsumerGroup(ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getConsumerGroup());
                                        consumeMessageContext.setNamespace(ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getNamespace());
                                        consumeMessageContext.setMq(this.messageQueue);
                                        consumeMessageContext.setMsgList(msgs);
                                        consumeMessageContext.setSuccess(false);
                                        consumeMessageContext.setProps(new HashMap());
                                        ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.executeHookBefore(consumeMessageContext);
                                    }

                                    long beginTimestamp = System.currentTimeMillis();
                                    ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
                                    boolean hasException = false;

                                    try {
                                        this.processQueue.getLockConsume().lock();
                                        if (this.processQueue.isDropped()) {
                                            ConsumeMessageOrderlyService.log.warn("consumeMessage, the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
                                            return;
                                        }

                                        status = ConsumeMessageOrderlyService.this.messageListener.consumeMessage(Collections.unmodifiableList(msgs), context);
                                    } catch (Throwable var23) {
                                        ConsumeMessageOrderlyService.log.warn("consumeMessage exception: {} Group: {} Msgs: {} MQ: {}", new Object[]{RemotingHelper.exceptionSimpleDesc(var23), ConsumeMessageOrderlyService.this.consumerGroup, msgs, this.messageQueue});
                                        hasException = true;
                                    } finally {
                                        this.processQueue.getLockConsume().unlock();
                                    }

                                    if (null == status || ConsumeOrderlyStatus.ROLLBACK == status || ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT == status) {
                                        ConsumeMessageOrderlyService.log.warn("consumeMessage Orderly return not OK, Group: {} Msgs: {} MQ: {}", new Object[]{ConsumeMessageOrderlyService.this.consumerGroup, msgs, this.messageQueue});
                                    }

                                    long consumeRT = System.currentTimeMillis() - beginTimestamp;
                                    if (null == status) {
                                        if (hasException) {
                                            returnType = ConsumeReturnType.EXCEPTION;
                                        } else {
                                            returnType = ConsumeReturnType.RETURNNULL;
                                        }
                                    } else if (consumeRT >= ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getConsumeTimeout() * 60L * 1000L) {
                                        returnType = ConsumeReturnType.TIME_OUT;
                                    } else if (ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT == status) {
                                        returnType = ConsumeReturnType.FAILED;
                                    } else if (ConsumeOrderlyStatus.SUCCESS == status) {
                                        returnType = ConsumeReturnType.SUCCESS;
                                    }

                                    if (ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                                        consumeMessageContext.getProps().put("ConsumeContextType", returnType.name());
                                    }

                                    if (null == status) {
                                        status = ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                                    }

                                    if (ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                                        consumeMessageContext.setStatus(status.toString());
                                        consumeMessageContext.setSuccess(ConsumeOrderlyStatus.SUCCESS == status || ConsumeOrderlyStatus.COMMIT == status);
                                        ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.executeHookAfter(consumeMessageContext);
                                    }

                                    ConsumeMessageOrderlyService.this.getConsumerStatsManager().incConsumeRT(ConsumeMessageOrderlyService.this.consumerGroup, this.messageQueue.getTopic(), consumeRT);
                                    continueConsume = ConsumeMessageOrderlyService.this.processConsumeResult(msgs, status, context, this);
                                } else {
                                    continueConsume = false;
                                }
                            }

                            return;
                        }
                    }
                }
            }
        }
    }
}