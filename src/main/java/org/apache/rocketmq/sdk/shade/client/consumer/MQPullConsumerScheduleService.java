package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.ThreadFactoryImpl;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MQPullConsumerScheduleService {
    private DefaultMQPullConsumer defaultMQPullConsumer;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private final InternalLogger log = ClientLogger.getLog();
    private final MessageQueueListener messageQueueListener = new MessageQueueListenerImpl();
    private final ConcurrentMap<MessageQueue, PullTaskImpl> taskTable = new ConcurrentHashMap();
    private int pullThreadNums = 20;
    private ConcurrentMap<String, PullTaskCallback> callbackTable = new ConcurrentHashMap();

    public MQPullConsumerScheduleService(String consumerGroup) {
        this.defaultMQPullConsumer = new DefaultMQPullConsumer(consumerGroup);
        this.defaultMQPullConsumer.setMessageModel(MessageModel.CLUSTERING);
    }

    public void putTask(String topic, Set<MessageQueue> mqNewSet) {
        Iterator<Map.Entry<MessageQueue, PullTaskImpl>> it = this.taskTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MessageQueue, PullTaskImpl> next = it.next();
            if (next.getKey().getTopic().equals(topic) && !mqNewSet.contains(next.getKey())) {
                next.getValue().setCancelled(true);
                it.remove();
            }
        }
        for (MessageQueue mq : mqNewSet) {
            if (!this.taskTable.containsKey(mq)) {
                PullTaskImpl command = new PullTaskImpl(mq);
                this.taskTable.put(mq, command);
                this.scheduledThreadPoolExecutor.schedule(command, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void start() throws MQClientException {
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(this.pullThreadNums, new ThreadFactoryImpl("PullMsgThread-" + this.defaultMQPullConsumer.getConsumerGroup()));
        this.defaultMQPullConsumer.setMessageQueueListener(this.messageQueueListener);
        this.defaultMQPullConsumer.start();
        this.log.info("MQPullConsumerScheduleService start OK, {} {}", this.defaultMQPullConsumer.getConsumerGroup(), this.callbackTable);
    }

    public void registerPullTaskCallback(String topic, PullTaskCallback callback) {
        this.callbackTable.put(topic, callback);
        this.defaultMQPullConsumer.registerMessageQueueListener(topic, null);
    }

    public void shutdown() {
        if (this.scheduledThreadPoolExecutor != null) {
            this.scheduledThreadPoolExecutor.shutdown();
        }
        if (this.defaultMQPullConsumer != null) {
            this.defaultMQPullConsumer.shutdown();
        }
    }

    public ConcurrentMap<String, PullTaskCallback> getCallbackTable() {
        return this.callbackTable;
    }

    public void setCallbackTable(ConcurrentHashMap<String, PullTaskCallback> callbackTable) {
        this.callbackTable = callbackTable;
    }

    public int getPullThreadNums() {
        return this.pullThreadNums;
    }

    public void setPullThreadNums(int pullThreadNums) {
        this.pullThreadNums = pullThreadNums;
    }

    public DefaultMQPullConsumer getDefaultMQPullConsumer() {
        return this.defaultMQPullConsumer;
    }

    public void setDefaultMQPullConsumer(DefaultMQPullConsumer defaultMQPullConsumer) {
        this.defaultMQPullConsumer = defaultMQPullConsumer;
    }

    public MessageModel getMessageModel() {
        return this.defaultMQPullConsumer.getMessageModel();
    }

    public void setMessageModel(MessageModel messageModel) {
        this.defaultMQPullConsumer.setMessageModel(messageModel);
    }

    class MessageQueueListenerImpl implements MessageQueueListener {
        MessageQueueListenerImpl() {
        }

        @Override
        public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {
            switch (MQPullConsumerScheduleService.this.defaultMQPullConsumer.getMessageModel()) {
                case BROADCASTING:
                    MQPullConsumerScheduleService.this.putTask(topic, mqAll);
                    return;
                case CLUSTERING:
                    MQPullConsumerScheduleService.this.putTask(topic, mqDivided);
                    return;
                default:
                    return;
            }
        }
    }

    public class PullTaskImpl implements Runnable {
        private final MessageQueue messageQueue;
        private volatile boolean cancelled = false;

        public PullTaskImpl(MessageQueue messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            String topic = this.messageQueue.getTopic();
            if (!isCancelled()) {
                PullTaskCallback pullTaskCallback = (PullTaskCallback) MQPullConsumerScheduleService.this.callbackTable.get(topic);
                if (pullTaskCallback != null) {
                    PullTaskContext context = new PullTaskContext();
                    context.setPullConsumer(MQPullConsumerScheduleService.this.defaultMQPullConsumer);
                    try {
                        pullTaskCallback.doPullTask(this.messageQueue, context);
                    } catch (Throwable e) {
                        context.setPullNextDelayTimeMillis(1000);
                        MQPullConsumerScheduleService.this.log.error("doPullTask Exception", e);
                    }
                    if (!isCancelled()) {
                        MQPullConsumerScheduleService.this.scheduledThreadPoolExecutor.schedule(this, (long) context.getPullNextDelayTimeMillis(), TimeUnit.MILLISECONDS);
                    } else {
                        MQPullConsumerScheduleService.this.log.warn("The Pull Task is cancelled after doPullTask, {}", this.messageQueue);
                    }
                } else {
                    MQPullConsumerScheduleService.this.log.warn("Pull Task Callback not exist , {}", topic);
                }
            } else {
                MQPullConsumerScheduleService.this.log.warn("The Pull Task is cancelled, {}", this.messageQueue);
            }
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public MessageQueue getMessageQueue() {
            return this.messageQueue;
        }
    }
}
