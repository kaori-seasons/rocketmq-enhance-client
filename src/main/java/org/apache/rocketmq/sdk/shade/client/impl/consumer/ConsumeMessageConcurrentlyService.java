package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.consumer.batch.TopicCache;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyByQueueContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyByTopicContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeReturnType;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.Pair;
import org.apache.rocketmq.sdk.shade.common.ThreadFactoryImpl;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.CMResult;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.springframework.util.backoff.FixedBackOff;

public class ConsumeMessageConcurrentlyService implements ConsumeMessageService {
    private static final InternalLogger log = ClientLogger.getLog();
    private final DefaultMQPushConsumerImpl defaultMQPushConsumerImpl;
    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final MessageListenerConcurrently messageListener;
    private final ThreadPoolExecutor consumeExecutor;
    private final String consumerGroup;
    private Thread batchConsumeThread;
    private final Object batchConsumeConditionVariable;
    private final ConcurrentMap<String, TopicCache> cachedMessages;
    private volatile boolean stopped = false;
    private final BlockingQueue<Runnable> consumeRequestQueue = new LinkedBlockingQueue();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ConsumeMessageScheduledThread_"));
    private final ScheduledExecutorService cleanExpireMsgExecutors = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("CleanExpireMsgScheduledThread_"));

    public ConsumeMessageConcurrentlyService(DefaultMQPushConsumerImpl defaultMQPushConsumerImpl, MessageListenerConcurrently messageListener) {
        this.defaultMQPushConsumerImpl = defaultMQPushConsumerImpl;
        this.messageListener = messageListener;
        this.defaultMQPushConsumer = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer();
        this.consumerGroup = this.defaultMQPushConsumer.getConsumerGroup();
        this.consumeExecutor = new ThreadPoolExecutor(this.defaultMQPushConsumer.getConsumeThreadMin(), this.defaultMQPushConsumer.getConsumeThreadMax(), 60000, TimeUnit.MILLISECONDS, this.consumeRequestQueue, new ThreadFactoryImpl("ConsumeMessageThread_"));
        if (this.defaultMQPushConsumer.getMaxBatchConsumeWaitTime() > 0) {
            this.batchConsumeThread = new Thread(new BatchConsumeTask());
            log.info("Consume message in batch mode, maxBatchAwaitTime={}ms", Long.valueOf(this.defaultMQPushConsumer.getMaxBatchConsumeWaitTime()));
        }
        this.batchConsumeConditionVariable = new Object();
        this.cachedMessages = new ConcurrentHashMap();
    }

    public class BatchConsumeTask implements Runnable {
        private BatchConsumeTask() {

        }

        @Override
        public void run() {
            while (!ConsumeMessageConcurrentlyService.this.stopped) {
                try {
                    ConsumeMessageConcurrentlyService.this.tryBatchConsume();
                    synchronized (ConsumeMessageConcurrentlyService.this.batchConsumeConditionVariable) {
                        ConsumeMessageConcurrentlyService.this.batchConsumeConditionVariable.wait(1000);
                    }
                } catch (Throwable e) {
                    ConsumeMessageConcurrentlyService.log.warn("Exception raised while schedule managing batch consuming", e);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        if (this.defaultMQPushConsumer.getMaxBatchConsumeWaitTime() > 0) {
            this.batchConsumeThread.start();
        }
        this.cleanExpireMsgExecutors.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                ConsumeMessageConcurrentlyService.this.cleanExpireMsg();
            }
        }, this.defaultMQPushConsumer.getConsumeTimeout(), this.defaultMQPushConsumer.getConsumeTimeout(), TimeUnit.MINUTES);
    }

    @Override
    public void shutdown() {
        this.stopped = true;
        if (null != this.batchConsumeThread) {
            this.batchConsumeThread.interrupt();
        }
        this.scheduledExecutorService.shutdown();
        this.consumeExecutor.shutdown();
        this.cleanExpireMsgExecutors.shutdown();
    }

    void tryBatchConsume() {
        for (Map.Entry<String, TopicCache> entry : this.cachedMessages.entrySet()) {
            while (true) {
                int size = entry.getValue().size();
                if (size >= this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize() || entry.getValue().elapsed() >= this.defaultMQPushConsumer.getMaxBatchConsumeWaitTime()) {
                    List<MessageExt> messages = new ArrayList<>();
                    entry.getValue().take(Math.min(size, this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize()), messages);
                    this.consumeExecutor.submit(new TopicBatchConsumeRequest(messages));
                }
            }
        }
    }

    @Override
    public void updateCorePoolSize(int corePoolSize) {
        if (corePoolSize > 0 && corePoolSize <= 32767 && corePoolSize < this.defaultMQPushConsumer.getConsumeThreadMax()) {
            this.consumeExecutor.setCorePoolSize(corePoolSize);
        }
    }

    @Override
    public void incCorePoolSize() {
    }

    @Override
    public void decCorePoolSize() {
    }

    @Override
    public int getCorePoolSize() {
        return this.consumeExecutor.getCorePoolSize();
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt msg, String brokerName) {
        ConsumeMessageDirectlyResult result = new ConsumeMessageDirectlyResult();
        result.setOrder(false);
        result.setAutoCommit(true);
        List<MessageExt> msgs = new ArrayList<>();
        msgs.add(msg);
        MessageQueue mq = new MessageQueue();
        mq.setBrokerName(brokerName);
        mq.setTopic(msg.getTopic());
        mq.setQueueId(msg.getQueueId());
        ConsumeConcurrentlyContext context = new ConsumeConcurrentlyByQueueContext(mq);
        this.defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, this.consumerGroup);
        long beginTime = System.currentTimeMillis();
        log.info("consumeMessageDirectly receive new message: {}", msg);
        try {
            ConsumeConcurrentlyStatus status = this.messageListener.consumeMessage(msgs, context);
            if (status != null) {
                switch (status) {
                    case CONSUME_SUCCESS:
                        result.setConsumeResult(CMResult.CR_SUCCESS);
                        break;
                    case RECONSUME_LATER:
                        result.setConsumeResult(CMResult.CR_LATER);
                        break;
                }
            } else {
                result.setConsumeResult(CMResult.CR_RETURN_NULL);
            }
        } catch (Throwable e) {
            result.setConsumeResult(CMResult.CR_THROW_EXCEPTION);
            result.setRemark(RemotingHelper.exceptionSimpleDesc(e));
            log.warn(String.format("consumeMessageDirectly exception: %s Group: %s Msgs: %s MQ: %s", RemotingHelper.exceptionSimpleDesc(e), this.consumerGroup, msgs, mq), e);
        }
        result.setSpentTimeMills(System.currentTimeMillis() - beginTime);
        log.info("consumeMessageDirectly Result: {}", result);
        return result;
    }

    @Override
    public void submitConsumeRequest(List<MessageExt> msgs, ProcessQueue processQueue, MessageQueue messageQueue, boolean dispatchToConsume) {
        if (msgs != null && !msgs.isEmpty()) {
            if (this.defaultMQPushConsumer.getMaxBatchConsumeWaitTime() > 0) {
                this.defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, this.defaultMQPushConsumer.getConsumerGroup());
                String topic = msgs.iterator().next().getTopic();
                if (!this.cachedMessages.containsKey(topic)) {
                    this.cachedMessages.putIfAbsent(topic, new TopicCache(topic));
                }
                this.cachedMessages.get(topic).put(msgs);
                synchronized (this.batchConsumeConditionVariable) {
                    this.batchConsumeConditionVariable.notify();
                }
                return;
            }
            int consumeBatchSize = this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();
            if (msgs.size() <= consumeBatchSize) {
                ConsumeRequest consumeRequest = new ConsumeRequest(msgs, processQueue, messageQueue);
                try {
                    this.consumeExecutor.submit(consumeRequest);
                } catch (RejectedExecutionException e) {
                    submitConsumeRequestLater(consumeRequest);
                }
            } else {
                int total = 0;
                while (total < msgs.size()) {
                    List<MessageExt> msgThis = new ArrayList<>(consumeBatchSize);
                    int i = 0;
                    while (i < consumeBatchSize && total < msgs.size()) {
                        msgThis.add(msgs.get(total));
                        i++;
                        total++;
                    }
                    ConsumeRequest consumeRequest2 = new ConsumeRequest(msgThis, processQueue, messageQueue);
                    try {
                        this.consumeExecutor.submit(consumeRequest2);
                    } catch (RejectedExecutionException e2) {
                        while (total < msgs.size()) {
                            msgThis.add(msgs.get(total));
                            total++;
                        }
                        submitConsumeRequestLater(consumeRequest2);
                    }
                }
            }
        }
    }

    public void resetRetryTopic(List<MessageExt> msgs) {
        String groupTopic = MixAll.getRetryTopic(this.consumerGroup);
        for (MessageExt msg : msgs) {
            String retryTopic = msg.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
            if (retryTopic != null && groupTopic.equals(msg.getTopic())) {
                msg.setTopic(retryTopic);
            }
        }
    }

    public void cleanExpireMsg() {
        for (Map.Entry<MessageQueue, ProcessQueue> next : this.defaultMQPushConsumerImpl.getRebalanceImpl().getProcessQueueTable().entrySet()) {
            next.getValue().cleanExpiredMsg(this.defaultMQPushConsumer);
        }
    }

    public void processConsumeResult(ConsumeConcurrentlyStatus status, ConsumeConcurrentlyContext context, ConsumeRequest consumeRequest) {
        int ackIndex = context.getAckIndex();
        if (!consumeRequest.getMsgs().isEmpty()) {
            switch (status) {
                case CONSUME_SUCCESS:
                    if (ackIndex >= consumeRequest.getMsgs().size()) {
                        ackIndex = consumeRequest.getMsgs().size() - 1;
                    }
                    int ok = ackIndex + 1;
                    int failed = consumeRequest.getMsgs().size() - ok;
                    getConsumerStatsManager().incConsumeOKTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long) ok);
                    getConsumerStatsManager().incConsumeFailedTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long) failed);
                    break;
                case RECONSUME_LATER:
                    ackIndex = -1;
                    getConsumerStatsManager().incConsumeFailedTPS(this.consumerGroup, consumeRequest.getMessageQueue().getTopic(), (long) consumeRequest.getMsgs().size());
                    break;
            }
            switch (this.defaultMQPushConsumer.getMessageModel()) {
                case BROADCASTING:
                    for (int i = ackIndex + 1; i < consumeRequest.getMsgs().size(); i++) {
                        log.warn("BROADCASTING, the message consume failed, drop it, {}", consumeRequest.getMsgs().get(i).toString());
                    }
                    break;
                case CLUSTERING:
                    List<MessageExt> msgBackFailed = new ArrayList<>(consumeRequest.getMsgs().size());
                    for (int i2 = ackIndex + 1; i2 < consumeRequest.getMsgs().size(); i2++) {
                        MessageExt msg = consumeRequest.getMsgs().get(i2);
                        if (!sendMessageBack(msg, context)) {
                            msg.setReconsumeTimes(msg.getReconsumeTimes() + 1);
                            msgBackFailed.add(msg);
                        }
                    }
                    if (!msgBackFailed.isEmpty()) {
                        consumeRequest.getMsgs().removeAll(msgBackFailed);
                        submitConsumeRequestLater(msgBackFailed, consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue());
                        break;
                    }
                    break;
            }
            long offset = consumeRequest.getProcessQueue().removeMessage(consumeRequest.getMsgs());
            if (offset >= 0 && !consumeRequest.getProcessQueue().isDropped()) {
                this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), offset, true);
            }
        }
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return this.defaultMQPushConsumerImpl.getConsumerStatsManager();
    }

    public boolean sendMessageBack(MessageExt msg, ConsumeConcurrentlyContext context) {
        int delayLevel = context.getDelayLevelWhenNextConsume();
        msg.setTopic(this.defaultMQPushConsumer.withNamespace(msg.getTopic()));
        try {
            this.defaultMQPushConsumerImpl.sendMessageBack(msg, delayLevel, context.getMessageQueue().getBrokerName());
            return true;
        } catch (Exception e) {
            log.error("sendMessageBack exception, group: " + this.consumerGroup + " msg: " + msg.toString(), (Throwable) e);
            return false;
        }
    }

    private void submitConsumeRequestLater(final List<MessageExt> msgs, final ProcessQueue processQueue, final MessageQueue messageQueue) {
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                ConsumeMessageConcurrentlyService.this.submitConsumeRequest(msgs, processQueue, messageQueue, true);
            }
        }, FixedBackOff.DEFAULT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void submitConsumeRequestLater(final ConsumeRequest consumeRequest) {
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                ConsumeMessageConcurrentlyService.this.consumeExecutor.submit(consumeRequest);
            }
        }, FixedBackOff.DEFAULT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public static String topicBrokerNameOf(MessageExt messageExt) {
        String topic;
        if (StringUtils.isEmpty(messageExt.getProperty(MessageConst.PROPERTY_FQN_TOPIC))) {
            topic = messageExt.getTopic();
        } else {
            topic = messageExt.getProperty(MessageConst.PROPERTY_FQN_TOPIC);
        }
        return topic + "@" + messageExt.getBrokerName();
    }

    public static String topicOf(MessageExt messageExt) {
        String fqn = messageExt.getProperty(MessageConst.PROPERTY_FQN_TOPIC);
        if (StringUtils.isEmpty(fqn)) {
            return messageExt.getTopic();
        }
        return fqn;
    }

    public class TopicBatchConsumeRequest implements Runnable {
        private final List<MessageExt> messages;
        private String topic;

        public TopicBatchConsumeRequest(List<MessageExt> messages) {
            this.messages = messages;
            if (null != this.messages && !this.messages.isEmpty()) {
                this.topic = this.messages.iterator().next().getTopic();
            }
        }

        private String msgIdsOf(Collection<MessageExt> messages) {
            StringBuilder sb = new StringBuilder();
            if (null == messages || messages.isEmpty()) {
                return sb.toString();
            }
            for (MessageExt message : messages) {
                if (0 == sb.length()) {
                    sb.append(message.getMsgId());
                } else {
                    sb.append(',').append(message.getMsgId());
                }
            }
            return sb.toString();
        }

        @Override
        public void run() {
            if (!this.messages.isEmpty() && null != this.topic) {
                try {
                    Map<Pair<String, Integer>, List<MessageExt>> groupBy = new HashMap<>();
                    for (MessageExt message : this.messages) {
                        Pair<String, Integer> key = new Pair<>(ConsumeMessageConcurrentlyService.topicBrokerNameOf(message), Integer.valueOf(message.getQueueId()));
                        if (!groupBy.containsKey(key)) {
                            groupBy.put(key, new ArrayList<MessageExt>());
                        }
                        groupBy.get(key).add(message);
                    }
                    Map<MessageQueue, ConsumeMessageContext> consumeMessageContextMap = new HashMap<>();
                    for (Map.Entry<Pair<String, Integer>, List<MessageExt>> entry : groupBy.entrySet()) {
                        MessageExt headMessage = entry.getValue().iterator().next();
                        assert null != headMessage;

                        MessageQueue messageQueue = new MessageQueue(ConsumeMessageConcurrentlyService.topicOf(headMessage), headMessage.getBrokerName(), entry.getKey().getObject2().intValue());
                        if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                            ConsumeMessageContext consumeMessageContext = new ConsumeMessageContext();
                            consumeMessageContext.setNamespace(ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getNamespace());
                            consumeMessageContext.setConsumerGroup(ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getConsumerGroup());
                            consumeMessageContext.setProps(new HashMap());
                            consumeMessageContext.setMq(messageQueue);
                            consumeMessageContext.setMsgList(entry.getValue());
                            consumeMessageContext.setSuccess(false);
                            consumeMessageContextMap.put(messageQueue, consumeMessageContext);
                            ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookBefore(consumeMessageContext);
                        }

                    }
                    long beginTimestamp = System.currentTimeMillis();
                    ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
                    ConsumeConcurrentlyContext context = new ConsumeConcurrentlyByTopicContext();
                    if (!this.messages.isEmpty()) {
                        for (MessageExt msg : this.messages) {
                            MessageAccessor.setConsumeStartTimeStamp(msg, String.valueOf(System.currentTimeMillis()));
                        }
                    }
                    ConsumeConcurrentlyStatus status = ConsumeMessageConcurrentlyService.this.messageListener.consumeMessage(Collections.unmodifiableList(this.messages), context);
                    long consumeRT = System.currentTimeMillis() - beginTimestamp;
                    if (null == status) {
                        if (0 != 0) {
                            returnType = ConsumeReturnType.EXCEPTION;
                        } else {
                            returnType = ConsumeReturnType.RETURNNULL;
                        }
                    } else if (consumeRT >= ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getConsumeTimeout() * 60 * 1000) {
                        returnType = ConsumeReturnType.TIME_OUT;
                    } else if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
                        returnType = ConsumeReturnType.FAILED;
                    } else if (ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status) {
                        returnType = ConsumeReturnType.SUCCESS;
                    }
                    if (null == status) {
                        ConsumeMessageConcurrentlyService.log.warn("consumeMessage return null, Group: {} Msgs: {}", ConsumeMessageConcurrentlyService.this.consumerGroup, this.messages);
                        status = ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                    Map<MessageQueue, Integer> ackMap = new HashMap<>();
                    if (ConsumeConcurrentlyStatus.CONSUME_SUCCESS != status) {
                        for (int i = 0; i < context.getAckIndex() && i < this.messages.size(); i++) {
                            MessageExt message2 = this.messages.get(i);
                            MessageQueue messageQueue2 = new MessageQueue(this.topic, message2.getBrokerName(), message2.getQueueId());
                            if (!ackMap.containsKey(messageQueue2)) {
                                ackMap.put(messageQueue2, 0);
                            }
                            ackMap.put(messageQueue2, Integer.valueOf(ackMap.get(messageQueue2).intValue() + 1));
                        }
                    }
                    for (Map.Entry<Pair<String, Integer>, List<MessageExt>> entry2 : groupBy.entrySet()) {
                        MessageExt headMessage2 = entry2.getValue().iterator().next();
                        assert null != headMessage2;

                        MessageQueue messageQueue3 = new MessageQueue(ConsumeMessageConcurrentlyService.topicOf(headMessage2), headMessage2.getBrokerName(), entry2.getKey().getObject2().intValue());
                        ConsumeConcurrentlyContext queueContext = new ConsumeConcurrentlyByQueueContext(messageQueue3);
                        if (ackMap.containsKey(messageQueue3)) {
                            queueContext.setAckIndex(ackMap.get(messageQueue3).intValue());
                        }
                        ConsumeMessageContext consumeMessageContext2 = consumeMessageContextMap.get(messageQueue3);
                        if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                            consumeMessageContext2.getProps().put(MixAll.CONSUME_CONTEXT_TYPE, returnType.name());
                            consumeMessageContext2.setStatus(status.toString());
                            consumeMessageContext2.setSuccess(ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status);
                            ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookAfter(consumeMessageContext2);
                        }
                        ConsumeMessageConcurrentlyService.this.getConsumerStatsManager().incConsumeRT(ConsumeMessageConcurrentlyService.this.consumerGroup, messageQueue3.getTopic(), consumeRT);
                        ProcessQueue processQueue = ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.getRebalanceImpl().getProcessQueueTable().get(messageQueue3);
                        if (null == processQueue || processQueue.isDropped()) {
                            ConsumeMessageConcurrentlyService.log.info("processQueue is dropped without process consume result. messageQueue={}, msgIdList={}", messageQueue3, msgIdsOf(entry2.getValue()));
                        } else {
                            ConsumeMessageConcurrentlyService.this.processConsumeResult(status, queueContext, new ConsumeRequest(entry2.getValue(), processQueue, messageQueue3));
                        }

                    }
                } catch (Throwable e) {
                    ConsumeMessageConcurrentlyService.log.error("[BUG]TopicBatchConsumeRequest raised an unexpected exception", e);
                }
            }
        }
    }

    public class ConsumeRequest implements Runnable {
        private final List<MessageExt> msgs;
        private final ProcessQueue processQueue;
        private final MessageQueue messageQueue;

        public ConsumeRequest(List<MessageExt> msgs, ProcessQueue processQueue, MessageQueue messageQueue) {
            this.msgs = msgs;
            this.processQueue = processQueue;
            this.messageQueue = messageQueue;
        }

        public List<MessageExt> getMsgs() {
            return this.msgs;
        }

        public ProcessQueue getProcessQueue() {
            return this.processQueue;
        }

        @Override
        public void run() {
            if (this.processQueue.isDropped()) {
                ConsumeMessageConcurrentlyService.log.info("the message queue not be able to consume, because it's dropped. group={} {}", ConsumeMessageConcurrentlyService.this.consumerGroup, this.messageQueue);
                return;
            }
            MessageListenerConcurrently listener = ConsumeMessageConcurrentlyService.this.messageListener;
            ConsumeConcurrentlyContext context = new ConsumeConcurrentlyByQueueContext(this.messageQueue);
            ConsumeConcurrentlyStatus status = null;
            ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.resetRetryAndNamespace(this.msgs, ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getConsumerGroup());
            ConsumeMessageContext consumeMessageContext = null;
            if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext = new ConsumeMessageContext();
                consumeMessageContext.setNamespace(ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getNamespace());
                consumeMessageContext.setConsumerGroup(ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getConsumerGroup());
                consumeMessageContext.setProps(new HashMap());
                consumeMessageContext.setMq(this.messageQueue);
                consumeMessageContext.setMsgList(this.msgs);
                consumeMessageContext.setSuccess(false);
                ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookBefore(consumeMessageContext);
            }
            long beginTimestamp = System.currentTimeMillis();
            boolean hasException = false;
            ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
            try {
                ConsumeMessageConcurrentlyService.this.resetRetryTopic(this.msgs);
                if (this.msgs != null && !this.msgs.isEmpty()) {
                    for (MessageExt msg : this.msgs) {
                        MessageAccessor.setConsumeStartTimeStamp(msg, String.valueOf(System.currentTimeMillis()));
                    }
                    status = listener.consumeMessage(Collections.unmodifiableList(this.msgs), context);
                }
            } catch (Throwable e) {
                ConsumeMessageConcurrentlyService.log.warn("consumeMessage exception: {} Group: {} Msgs: {} MQ: {}", RemotingHelper.exceptionSimpleDesc(e), ConsumeMessageConcurrentlyService.this.consumerGroup, this.msgs, this.messageQueue);
                hasException = true;
            }
            long consumeRT = System.currentTimeMillis() - beginTimestamp;
            if (null == status) {
                if (hasException) {
                    returnType = ConsumeReturnType.EXCEPTION;
                } else {
                    returnType = ConsumeReturnType.RETURNNULL;
                }
            } else if (consumeRT >= ConsumeMessageConcurrentlyService.this.defaultMQPushConsumer.getConsumeTimeout() * 60 * 1000) {
                returnType = ConsumeReturnType.TIME_OUT;
            } else if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
                returnType = ConsumeReturnType.FAILED;
            } else if (ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status) {
                returnType = ConsumeReturnType.SUCCESS;
            }
            if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext.getProps().put(MixAll.CONSUME_CONTEXT_TYPE, returnType.name());
            }
            if (null == status) {
                ConsumeMessageConcurrentlyService.log.warn("consumeMessage return null, Group: {} Msgs: {} MQ: {}", ConsumeMessageConcurrentlyService.this.consumerGroup, this.msgs, this.messageQueue);
                status = ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext.setStatus(status.toString());
                consumeMessageContext.setSuccess(ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status);
                ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookAfter(consumeMessageContext);
            }
            ConsumeMessageConcurrentlyService.this.getConsumerStatsManager().incConsumeRT(ConsumeMessageConcurrentlyService.this.consumerGroup, this.messageQueue.getTopic(), consumeRT);
            if (!this.processQueue.isDropped()) {
                ConsumeMessageConcurrentlyService.this.processConsumeResult(status, context, this);
            } else {
                ConsumeMessageConcurrentlyService.log.warn("processQueue is dropped without process consume result. messageQueue={}, msgs={}", this.messageQueue, this.msgs);
            }
        }

        public MessageQueue getMessageQueue() {
            return this.messageQueue;
        }
    }
}
