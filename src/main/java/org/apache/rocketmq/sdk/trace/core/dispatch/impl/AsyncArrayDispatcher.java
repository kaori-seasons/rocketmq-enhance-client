package org.apache.rocketmq.sdk.trace.core.dispatch.impl;

import org.apache.rocketmq.sdk.shade.client.common.ThreadLocalIndex;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.sdk.shade.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.sdk.shade.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.client.producer.SendCallback;
import org.apache.rocketmq.sdk.shade.client.producer.SendResult;
import org.apache.rocketmq.sdk.shade.common.ThreadFactoryImpl;
import org.apache.rocketmq.sdk.shade.common.acl.SessionCredentials;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceConstants;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceContext;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDataEncoder;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDispatcherType;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceTransferBean;
import org.apache.rocketmq.sdk.trace.core.dispatch.AsyncDispatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.util.backoff.FixedBackOff;

public class AsyncArrayDispatcher implements AsyncDispatcher {
    private static final InternalLogger clientlog = ClientLogger.getLog();
    private final int queueSize;
    private final int batchSize;
    private final DefaultMQProducer traceProducer;
    private final ThreadPoolExecutor traceExecuter;
    private Thread worker;
    private ArrayBlockingQueue<Runnable> appenderQueue;
    private volatile Thread shutDownHook;
    private String dispatcherType;
    private DefaultMQProducerImpl hostProducer;
    private DefaultMQPushConsumerImpl hostConsumer;
    private volatile boolean stopped = false;
    private volatile ThreadLocalIndex sendWhichQueue = new ThreadLocalIndex();
    private String dispatcherId = UUID.randomUUID().toString();
    private TraceProducerFactory factory = new TraceProducerFactory();
    private AtomicLong discardCount = new AtomicLong(0);
    private ArrayBlockingQueue<RMQTraceContext> traceContextQueue = new ArrayBlockingQueue<>(1024);
    
    public AsyncArrayDispatcher(Properties properties, SessionCredentials sessionCredentials) throws MQClientException {
        this.dispatcherType = properties.getProperty(RMQTraceConstants.TraceDispatcherType);
        int queueSize = 1 << (32 - Integer.numberOfLeadingZeros(Integer.parseInt(properties.getProperty(RMQTraceConstants.AsyncBufferSize, "2048")) - 1));
        this.queueSize = queueSize;
        this.batchSize = Integer.parseInt(properties.getProperty(RMQTraceConstants.MaxBatchNum, CustomBooleanEditor.VALUE_1));
        this.appenderQueue = new ArrayBlockingQueue<>(queueSize);
        this.traceExecuter = new ThreadPoolExecutor(10, 20, 60000, TimeUnit.MILLISECONDS, this.appenderQueue, new ThreadFactoryImpl("MQTraceSendThread_"));
        this.traceProducer = this.factory.getTraceDispatcherProducer(properties, sessionCredentials);
    }

    public DefaultMQProducerImpl getHostProducer() {
        return this.hostProducer;
    }

    public void setHostProducer(DefaultMQProducerImpl hostProducer) {
        this.hostProducer = hostProducer;
    }

    public DefaultMQPushConsumerImpl getHostConsumer() {
        return this.hostConsumer;
    }

    public void setHostConsumer(DefaultMQPushConsumerImpl hostConsumer) {
        this.hostConsumer = hostConsumer;
    }

    @Override
    public void start() throws MQClientException {
        this.factory.registerTraceDispatcher(this.dispatcherId);
        this.worker = new Thread(new AsyncRunnable(), "MQ-AsyncArrayDispatcher-Thread-" + this.dispatcherId);
        this.worker.setDaemon(true);
        this.worker.start();
        registerShutDownHook();
    }

    @Override
    public boolean append(Object ctx) {
        boolean result = this.traceContextQueue.offer((RMQTraceContext) ctx);
        if (!result) {
            clientlog.info("buffer full" + this.discardCount.incrementAndGet() + " ,context is " + ctx);
        }
        return result;
    }

    @Override
    public void flush() throws IOException {
        long end = System.currentTimeMillis() + 500;
        while (true) {
            if (this.traceContextQueue.size() <= 0 && (this.appenderQueue.size() <= 0 || System.currentTimeMillis() > end)) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        clientlog.info("------end trace send " + this.traceContextQueue.size() + "   " + this.appenderQueue.size());
    }

    @Override
    public void shutdown() {
        this.stopped = true;
        this.traceExecuter.shutdown();
        this.factory.unregisterTraceDispatcher(this.dispatcherId);
        removeShutdownHook();
    }

    public void registerShutDownHook() {
        if (this.shutDownHook == null) {
            this.shutDownHook = new Thread(new Runnable() {
                private volatile boolean hasShutdown = false;

                public void run() {
                    synchronized(this) {
                        if (!this.hasShutdown) {
                            try {
                                AsyncArrayDispatcher.this.flush();
                            } catch (IOException var4) {
                                AsyncArrayDispatcher.clientlog.error("system mqtrace hook shutdown failed ,maybe loss some trace data");
                            }
                        }

                    }
                }
            }, "ShutdownHookMQTrace");
            Runtime.getRuntime().addShutdownHook(this.shutDownHook);
        }

    }

    public void removeShutdownHook() {
        if (this.shutDownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutDownHook);
        }
    }

    class AsyncRunnable implements Runnable {
        private boolean stopped;

        AsyncRunnable() {
        }

        @Override
        public void run() {
            while (!this.stopped) {
                List<RMQTraceContext> contexts = new ArrayList<>(AsyncArrayDispatcher.this.batchSize);
                for (int i = 0; i < AsyncArrayDispatcher.this.batchSize; i++) {
                    RMQTraceContext context = null;
                    try {
                        context = (RMQTraceContext) AsyncArrayDispatcher.this.traceContextQueue.poll(5, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }
                    if (context == null) {
                        break;
                    }
                    contexts.add(context);
                }
                if (contexts.size() > 0) {
                    AsyncArrayDispatcher.this.traceExecuter.submit(new AsyncAppenderRequest(contexts));
                } else if (AsyncArrayDispatcher.this.stopped) {
                    this.stopped = true;
                }
            }
        }
    }

    class AsyncAppenderRequest implements Runnable {
        List<RMQTraceContext> contextList;

        public AsyncAppenderRequest(List<RMQTraceContext> contextList) {
            if (contextList != null) {
                this.contextList = contextList;
            } else {
                this.contextList = new ArrayList(1);
            }
        }

        @Override
        public void run() {
            sendTraceData(this.contextList);
        }

        public void sendTraceData(List<RMQTraceContext> contextList) {
            Map<String, List<RMQTraceTransferBean>> transBeanMap = new HashMap<>();
            for (RMQTraceContext context : contextList) {
                String key = context.getTraceBeans().get(0).getTopic() + (char) 1 + context.getRegionId();
                List<RMQTraceTransferBean> transBeanList = transBeanMap.get(key);
                if (transBeanList == null) {
                    transBeanList = new ArrayList<>();
                    transBeanMap.put(key, transBeanList);
                }
                transBeanList.add(RMQTraceDataEncoder.encoderFromContextBean(context));
            }
            for (Map.Entry<String, List<RMQTraceTransferBean>> entry : transBeanMap.entrySet()) {
                String[] key2 = entry.getKey().split(String.valueOf((char) 1));
                flushData(entry.getValue(), key2[0], key2[1]);
            }
        }

        private void flushData(List<RMQTraceTransferBean> transBeanList, String topic, String currentRegionId) {
            if (transBeanList.size() != 0) {
                StringBuilder buffer = new StringBuilder(1024);
                int count = 0;
                Set<String> keySet = new HashSet<>();
                for (RMQTraceTransferBean bean : transBeanList) {
                    keySet.addAll(bean.getTransKey());
                    buffer.append(bean.getTransData());
                    count++;
                    if (buffer.length() >= AsyncArrayDispatcher.this.traceProducer.getMaxMessageSize()) {
                        sendTraceDataByMQ(keySet, buffer.toString(), topic, currentRegionId);
                        buffer.delete(0, buffer.length());
                        keySet.clear();
                        count = 0;
                    }
                }
                if (count > 0) {
                    sendTraceDataByMQ(keySet, buffer.toString(), topic, currentRegionId);
                }
                transBeanList.clear();
            }
        }

        private void sendTraceDataByMQ(Set<String> keySet, final String data, String dataTopic, String currentRegionId) {
            Message message = new Message("RMQ_SYS_TRACE_TOPIC", data.getBytes());
            message.getProperties().put("originTopic", dataTopic);
            message.setKeys(keySet);
            try {
                Set<String> traceBrokerSet = tryGetMessageQueueBrokerSet(AsyncArrayDispatcher.this.traceProducer.getDefaultMQProducerImpl(), "RMQ_SYS_TRACE_TOPIC");
                SendCallback callback = new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                    }

                    @Override
                    public void onException(Throwable e) {
                        AsyncArrayDispatcher.clientlog.info("send trace data ,the traceData is " + data);
                    }
                };
                if (traceBrokerSet.isEmpty()) {
                    AsyncArrayDispatcher.this.traceProducer.send(message, callback, FixedBackOff.DEFAULT_INTERVAL);
                } else {
                    AsyncArrayDispatcher.this.traceProducer.send(message, new MessageQueueSelector() {
                        @Override
                        public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                            Set<String> brokerSet = (Set) arg;
                            List<MessageQueue> filterMqs = new ArrayList<>();
                            for (MessageQueue queue : mqs) {
                                if (brokerSet.contains(queue.getBrokerName())) {
                                    filterMqs.add(queue);
                                }
                            }
                            int pos = Math.abs(AsyncArrayDispatcher.this.sendWhichQueue.getAndIncrement()) % filterMqs.size();
                            if (pos < 0) {
                                pos = 0;
                            }
                            return filterMqs.get(pos);
                        }
                    }, traceBrokerSet, callback);
                }
            } catch (MQClientException e) {
                AsyncArrayDispatcher.clientlog.error("send trace data exc,the traceData is" + data);
            } catch (RemotingException e2) {
                AsyncArrayDispatcher.clientlog.error("send trace data exc,the traceData is" + data);
            } catch (InterruptedException e3) {
                AsyncArrayDispatcher.clientlog.error("send trace data exc,the traceData is" + data);
            }
        }

        private Set<String> getBrokerSetByTopic(String topic) {
            Set<String> brokerSet = new HashSet<>();
            if (!(AsyncArrayDispatcher.this.dispatcherType == null || !AsyncArrayDispatcher.this.dispatcherType.equals(RMQTraceDispatcherType.PRODUCER.name()) || AsyncArrayDispatcher.this.hostProducer == null)) {
                brokerSet = tryGetMessageQueueBrokerSet(AsyncArrayDispatcher.this.hostProducer, topic);
            }
            if (!(AsyncArrayDispatcher.this.dispatcherType == null || !AsyncArrayDispatcher.this.dispatcherType.equals(RMQTraceDispatcherType.CONSUMER.name()) || AsyncArrayDispatcher.this.hostConsumer == null)) {
                brokerSet = tryGetMessageQueueBrokerSet(AsyncArrayDispatcher.this.hostConsumer, topic);
            }
            return brokerSet;
        }

        private Set<String> tryGetMessageQueueBrokerSet(DefaultMQProducerImpl producer, String topic) {
            Set<String> brokerSet = new HashSet<>();
            TopicPublishInfo topicPublishInfo = producer.getTopicPublishInfoTable().get(topic);
            if (null == topicPublishInfo || !topicPublishInfo.ok()) {
                producer.getTopicPublishInfoTable().putIfAbsent(topic, new TopicPublishInfo());
                producer.getmQClientFactory().updateTopicRouteInfoFromNameServer(topic);
                topicPublishInfo = producer.getTopicPublishInfoTable().get(topic);
            }
            if (topicPublishInfo.isHaveTopicRouterInfo() || topicPublishInfo.ok()) {
                for (MessageQueue queue : topicPublishInfo.getMessageQueueList()) {
                    brokerSet.add(queue.getBrokerName());
                }
            }
            return brokerSet;
        }

        private Set<String> tryGetMessageQueueBrokerSet(DefaultMQPushConsumerImpl consumer, String topic) {
            Set<String> brokerSet = new HashSet<>();
            try {
                for (MessageQueue queue : consumer.fetchSubscribeMessageQueues(topic)) {
                    brokerSet.add(queue.getBrokerName());
                }
            } catch (MQClientException e) {
                AsyncArrayDispatcher.clientlog.info("fetch message queue failed, the topic is {}", topic);
            }
            return brokerSet;
        }
    }
}
