package org.apache.rocketmq.sdk.shade.client.impl.factory;

import org.apache.rocketmq.sdk.shade.client.ClientConfig;
import org.apache.rocketmq.sdk.shade.client.admin.MQAdminExtInner;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.ClientRemotingProcessor;
import org.apache.rocketmq.sdk.shade.client.impl.FindBrokerResult;
import org.apache.rocketmq.sdk.shade.client.impl.MQAdminImpl;
import org.apache.rocketmq.sdk.shade.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.sdk.shade.client.impl.MQClientManager;
import org.apache.rocketmq.sdk.shade.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.sdk.shade.client.impl.producer.MQProducerInner;
import org.apache.rocketmq.sdk.shade.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.sdk.shade.common.MQVersion;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.ServiceState;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.constant.PermName;
import org.apache.rocketmq.sdk.shade.common.filter.ExpressionType;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumerData;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.BrokerData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.QueueData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.rocketmq.sdk.shade.client.impl.consumer.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.backoff.FixedBackOff;

public class MQClientInstance {
    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private final InternalLogger log;
    private final ClientConfig clientConfig;
    private final int instanceIndex;
    private final String clientId;
    private final long bootTimestamp;
    private final ConcurrentMap<String, MQProducerInner> producerTable;
    private final ConcurrentMap<String, MQConsumerInner> consumerTable;
    private final ConcurrentMap<String, MQAdminExtInner> adminExtTable;
    private final NettyClientConfig nettyClientConfig;
    private final MQClientAPIImpl mQClientAPIImpl;
    private final MQAdminImpl mQAdminImpl;
    private final ConcurrentMap<String, TopicRouteData> topicRouteTable;
    private final Lock lockNamesrv;
    private final Lock lockHeartbeat;
    private final ConcurrentMap<String, HashMap<Long, String>> brokerAddrTable;
    private final ConcurrentMap<String, HashMap<String, Integer>> brokerVersionTable;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ClientRemotingProcessor clientRemotingProcessor;
    private final PullMessageService pullMessageService;
    private final RebalanceService rebalanceService;
    private final DefaultMQProducer defaultMQProducer;
    private final ConsumerStatsManager consumerStatsManager;
    private final AtomicLong sendHeartbeatTimesTotal;
    private ServiceState serviceState;
    private Random random;

    public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId) {
        this(clientConfig, instanceIndex, clientId, null);
    }

    public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId, RPCHook rpcHook) {
        this.log = ClientLogger.getLog();
        this.bootTimestamp = System.currentTimeMillis();
        this.producerTable = new ConcurrentHashMap();
        this.consumerTable = new ConcurrentHashMap();
        this.adminExtTable = new ConcurrentHashMap();
        this.topicRouteTable = new ConcurrentHashMap();
        this.lockNamesrv = new ReentrantLock();
        this.lockHeartbeat = new ReentrantLock();
        this.brokerAddrTable = new ConcurrentHashMap();
        this.brokerVersionTable = new ConcurrentHashMap();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "MQClientFactoryScheduledThread");
            }
        });
        this.sendHeartbeatTimesTotal = new AtomicLong(0);
        this.serviceState = ServiceState.CREATE_JUST;
        this.random = new Random();
        this.clientConfig = clientConfig;
        this.instanceIndex = instanceIndex;
        this.nettyClientConfig = new NettyClientConfig();
        this.nettyClientConfig.setClientCallbackExecutorThreads(clientConfig.getClientCallbackExecutorThreads());
        this.nettyClientConfig.setUseTLS(clientConfig.isUseTLS());
        this.clientRemotingProcessor = new ClientRemotingProcessor(this);
        this.mQClientAPIImpl = new MQClientAPIImpl(this.nettyClientConfig, this.clientRemotingProcessor, rpcHook, clientConfig);
        if (this.clientConfig.getNamesrvAddr() != null) {
            this.mQClientAPIImpl.updateNameServerAddressList(this.clientConfig.getNamesrvAddr());
            this.log.info("user specified name server address: {}", this.clientConfig.getNamesrvAddr());
        }
        if (this.clientConfig.getProxyAddr() != null) {
            this.mQClientAPIImpl.updateProxyAddressList(this.clientConfig.getProxyAddr());
            this.log.info("user specified proxy address: {}", this.clientConfig.getProxyAddr());
        }
        this.clientId = clientId;
        this.mQAdminImpl = new MQAdminImpl(this);
        this.pullMessageService = new PullMessageService(this);
        this.rebalanceService = new RebalanceService(this);
        this.defaultMQProducer = new DefaultMQProducer(MixAll.CLIENT_INNER_PRODUCER_GROUP);
        this.defaultMQProducer.resetClientConfig(clientConfig);
        this.consumerStatsManager = new ConsumerStatsManager(this.scheduledExecutorService);
        this.log.info("Created a new client Instance, InstanceIndex:{}, ClientID:{}, ClientConfig:{}, ClientVersion:{}, SerializerType:{}", Integer.valueOf(this.instanceIndex), this.clientId, this.clientConfig, MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION), RemotingCommand.getSerializeTypeConfigInThisServer());
    }

    public static TopicPublishInfo topicRouteData2TopicPublishInfo(String topic, TopicRouteData route) {
        TopicPublishInfo info = new TopicPublishInfo();
        info.setTopicRouteData(route);
        if (route.getOrderTopicConf() == null || route.getOrderTopicConf().length() <= 0) {
            List<QueueData> qds = route.getQueueDatas();
            Collections.sort(qds);
            for (QueueData qd : qds) {
                if (PermName.isWriteable(qd.getPerm())) {
                    BrokerData brokerData = null;
                    Iterator<BrokerData> it = route.getBrokerDatas().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        BrokerData bd = it.next();
                        if (bd.getBrokerName().equals(qd.getBrokerName())) {
                            brokerData = bd;
                            break;
                        }
                    }
                    if (null != brokerData && brokerData.getBrokerAddrs().containsKey(0L)) {
                        for (int i = 0; i < qd.getWriteQueueNums(); i++) {
                            info.getMessageQueueList().add(new MessageQueue(topic, qd.getBrokerName(), i));
                        }
                    }
                }
            }
            info.setOrderTopic(false);
        } else {
            for (String broker : route.getOrderTopicConf().split(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR)) {
                String[] item = broker.split(":");
                int nums = Integer.parseInt(item[1]);
                for (int i2 = 0; i2 < nums; i2++) {
                    info.getMessageQueueList().add(new MessageQueue(topic, item[0], i2));
                }
            }
            info.setOrderTopic(true);
        }
        return info;
    }

    public static Set<MessageQueue> topicRouteData2TopicSubscribeInfo(String topic, TopicRouteData route) {
        Set<MessageQueue> mqList = new HashSet<>();
        for (QueueData qd : route.getQueueDatas()) {
            if (PermName.isReadable(qd.getPerm())) {
                for (int i = 0; i < qd.getReadQueueNums(); i++) {
                    mqList.add(new MessageQueue(topic, qd.getBrokerName(), i));
                }
            }
        }
        return mqList;
    }

    public void start() throws MQClientException {
        synchronized (this) {
            switch (this.serviceState) {
                case CREATE_JUST:
                    this.serviceState = ServiceState.START_FAILED;
                    this.mQClientAPIImpl.start();
                    startScheduledTask();
                    this.pullMessageService.start();
                    this.rebalanceService.start();
                    this.defaultMQProducer.getDefaultMQProducerImpl().start(false);
                    this.log.info("the client factory [{}] start OK", this.clientId);
                    this.serviceState = ServiceState.RUNNING;
                    break;
                case START_FAILED:
                    throw new MQClientException("The Factory object[" + getClientId() + "] has been created before, and failed.", (Throwable) null);
            }
        }
    }

    private void startScheduledTask() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MQClientInstance.this.updateTopicRouteInfoFromNameServer();
                } catch (Exception e) {
                    MQClientInstance.this.log.error("ScheduledTask updateTopicRouteInfoFromNameServer exception", (Throwable) e);
                }
            }
        }, 10, (long) this.clientConfig.getPollNameServerInterval(), TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MQClientInstance.this.cleanOfflineBroker();
                    MQClientInstance.this.sendHeartbeatToAllBrokerWithLock();
                } catch (Exception e) {
                    MQClientInstance.this.log.error("ScheduledTask sendHeartbeatToAllBroker exception", (Throwable) e);
                }
            }
        }, 1000, (long) this.clientConfig.getHeartbeatBrokerInterval(), TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MQClientInstance.this.persistAllConsumerOffset();
                } catch (Exception e) {
                    MQClientInstance.this.log.error("ScheduledTask persistAllConsumerOffset exception", (Throwable) e);
                }
            }
        }, 10000, (long) this.clientConfig.getPersistConsumerOffsetInterval(), TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MQClientInstance.this.adjustThreadPool();
                } catch (Exception e) {
                    MQClientInstance.this.log.error("ScheduledTask adjustThreadPool exception", (Throwable) e);
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public String getClientId() {
        return this.clientId;
    }

    public void updateTopicRouteInfoFromNameServer() {
        Set<SubscriptionData> subList;
        Set<String> topicList = new HashSet<>();
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            MQConsumerInner impl = entry.getValue();
            if (!(impl == null || (subList = impl.subscriptions()) == null)) {
                for (SubscriptionData subData : subList) {
                    topicList.add(subData.getTopic());
                }
            }
        }
        for (Map.Entry<String, MQProducerInner> entry2 : this.producerTable.entrySet()) {
            MQProducerInner impl2 = entry2.getValue();
            if (impl2 != null) {
                topicList.addAll(impl2.getPublishTopicList());
            }
        }
        for (String topic : topicList) {
            updateTopicRouteInfoFromNameServer(topic);
        }
    }

    public void cleanOfflineBroker() {
        try {
            if (this.lockNamesrv.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                ConcurrentHashMap<String, HashMap<Long, String>> updatedTable = new ConcurrentHashMap<>();
                Iterator<Map.Entry<String, HashMap<Long, String>>> itBrokerTable = this.brokerAddrTable.entrySet().iterator();
                while (itBrokerTable.hasNext()) {
                    Map.Entry<String, HashMap<Long, String>> entry = itBrokerTable.next();
                    String brokerName = entry.getKey();
                    HashMap<Long, String> oneTable = entry.getValue();
                    HashMap<Long, String> cloneAddrTable = new HashMap<>();
                    cloneAddrTable.putAll(oneTable);
                    Iterator<Map.Entry<Long, String>> it = cloneAddrTable.entrySet().iterator();
                    while (it.hasNext()) {
                        String addr = it.next().getValue();
                        if (!isBrokerAddrExistInTopicRouteTable(addr)) {
                            it.remove();
                            this.log.info("the broker addr[{} {}] is offline, remove it", brokerName, addr);
                        }
                    }
                    if (cloneAddrTable.isEmpty()) {
                        itBrokerTable.remove();
                        this.log.info("the broker[{}] name's host is offline, remove it", brokerName);
                    } else {
                        updatedTable.put(brokerName, cloneAddrTable);
                    }
                }
                if (!updatedTable.isEmpty()) {
                    this.brokerAddrTable.putAll(updatedTable);
                }
                this.lockNamesrv.unlock();
            }
        } catch (InterruptedException e) {
            this.log.warn("cleanOfflineBroker Exception", (Throwable) e);
        }
    }

    public void checkClientInBroker() throws MQClientException {
        String addr;
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            Set<SubscriptionData> subscriptionInner = entry.getValue().subscriptions();
            if (subscriptionInner != null && !subscriptionInner.isEmpty()) {
                for (SubscriptionData subscriptionData : subscriptionInner) {
                    if (!ExpressionType.isTagType(subscriptionData.getExpressionType()) && (addr = findBrokerAddrByTopic(subscriptionData.getTopic())) != null) {
                        try {
                            getMQClientAPIImpl().checkClientInBroker(addr, entry.getKey(), this.clientId, subscriptionData, LOCK_TIMEOUT_MILLIS);
                        } catch (Exception e) {
                            if (e instanceof MQClientException) {
                                throw ((MQClientException) e);
                            }
                            throw new MQClientException("Check client in broker error, maybe because you use " + subscriptionData.getExpressionType() + " to filter message, but server has not been upgraded to support!This error would not affect the launch of consumer, but may has impact on message receiving if you have use the new features which are not supported by server, please check the log!", e);
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    public void sendHeartbeatToAllBrokerWithTimedLock() {
        try {
            if (this.lockHeartbeat.tryLock(3000L, TimeUnit.MILLISECONDS)) {
                try {
                    sendHeartbeatToAllBroker();
                    uploadFilterClassSource();
                } catch (Exception e) {
                    this.log.error("sendHeartbeatToAllBroker exception", e);
                } finally {
                    this.lockHeartbeat.unlock();
                }
            } else {
                this.log.warn("lock heartBeat, but failed.");
            }
        } catch (InterruptedException e) {
            this.log.warn("sendHeartbeatToAllBrokerWithTimedLock exception", e);
        }
    }

    public void sendHeartbeatToAllBrokerWithLock() {
        if (this.lockHeartbeat.tryLock()) {
            try {
                sendHeartbeatToAllBroker();
                uploadFilterClassSource();
            } catch (Exception e) {
                this.log.error("sendHeartbeatToAllBroker exception", (Throwable) e);
            } finally {
                this.lockHeartbeat.unlock();
            }
        } else {
            this.log.warn("lock heartBeat, but failed.");
        }
    }

    public void persistAllConsumerOffset() {
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            entry.getValue().persistConsumerOffset();
        }
    }

    public void adjustThreadPool() {
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            MQConsumerInner impl = entry.getValue();
            if (impl != null) {
                try {
                    if (impl instanceof DefaultMQPushConsumerImpl) {
                        ((DefaultMQPushConsumerImpl) impl).adjustThreadPool();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public boolean updateTopicRouteInfoFromNameServer(String topic) {
        return updateTopicRouteInfoFromNameServer(topic, false, null);
    }

    private boolean isBrokerAddrExistInTopicRouteTable(String addr) {
        for (Map.Entry<String, TopicRouteData> entry : this.topicRouteTable.entrySet()) {
            for (BrokerData bd : entry.getValue().getBrokerDatas()) {
                if (bd.getBrokerAddrs() != null && bd.getBrokerAddrs().containsValue(addr)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendHeartbeatToAllBroker() {
        HeartbeatData heartbeatData = prepareHeartbeatData();
        boolean producerEmpty = heartbeatData.getProducerDataSet().isEmpty();
        boolean consumerEmpty = heartbeatData.getConsumerDataSet().isEmpty();
        if (producerEmpty && consumerEmpty) {
            this.log.warn("sending heartbeat, but no consumer and no producer");
        } else if (!this.brokerAddrTable.isEmpty()) {
            long times = this.sendHeartbeatTimesTotal.getAndIncrement();
            for (Map.Entry<String, HashMap<Long, String>> entry : this.brokerAddrTable.entrySet()) {
                String brokerName = entry.getKey();
                HashMap<Long, String> oneTable = entry.getValue();
                if (oneTable != null) {
                    for (Map.Entry<Long, String> entry1 : oneTable.entrySet()) {
                        Long id = entry1.getKey();
                        String addr = entry1.getValue();
                        if (addr != null && (!consumerEmpty || id.longValue() == 0)) {
                            try {
                                int version = this.mQClientAPIImpl.sendHearbeat(addr, heartbeatData, LOCK_TIMEOUT_MILLIS);
                                if (!this.brokerVersionTable.containsKey(brokerName)) {
                                    this.brokerVersionTable.put(brokerName, new HashMap<String, Integer>(4));
                                }
                                this.brokerVersionTable.get(brokerName).put(addr, Integer.valueOf(version));
                                if (times % 20 == 0) {
                                    this.log.info("send heart beat to broker[{} {} {}] success", brokerName, id, addr);
                                    this.log.info(heartbeatData.toString());
                                }
                            } catch (Exception e) {
                                if (isBrokerInNameServer(addr)) {
                                    this.log.info("send heart beat to broker[{} {} {}] failed", brokerName, id, addr);
                                } else {
                                    this.log.info("send heart beat to broker[{} {} {}] exception, because the broker not up, forget it", brokerName, id, addr);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void uploadFilterClassSource() {
        for (Map.Entry<String, MQConsumerInner> next : this.consumerTable.entrySet()) {
            MQConsumerInner consumer = next.getValue();
            if (ConsumeType.CONSUME_PASSIVELY == consumer.consumeType()) {
                for (SubscriptionData sub : consumer.subscriptions()) {
                    if (sub.isClassFilterMode() && sub.getFilterClassSource() != null) {
                        try {
                            uploadFilterClassToAllFilterServer(consumer.groupName(), sub.getSubString(), sub.getTopic(), sub.getFilterClassSource());
                        } catch (Exception e) {
                            this.log.error("uploadFilterClassToAllFilterServer Exception", (Throwable) e);
                        }
                    }
                }
            }
        }
    }

    public boolean updateTopicRouteInfoFromNameServer(String topic, boolean isDefault, DefaultMQProducer defaultMQProducer) {
        TopicRouteData topicRouteData;
        try {
            if (this.lockNamesrv.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                if (!isDefault || defaultMQProducer == null) {
                    topicRouteData = this.mQClientAPIImpl.getTopicRouteInfoFromNameServer(topic, LOCK_TIMEOUT_MILLIS);
                } else {
                    topicRouteData = this.mQClientAPIImpl.getDefaultTopicRouteInfoFromNameServer(defaultMQProducer.getCreateTopicKey(), LOCK_TIMEOUT_MILLIS);
                    if (topicRouteData != null) {
                        for (QueueData data : topicRouteData.getQueueDatas()) {
                            try {
                                int queueNums = Math.min(defaultMQProducer.getDefaultTopicQueueNums(), data.getReadQueueNums());
                                data.setReadQueueNums(queueNums);
                                data.setWriteQueueNums(queueNums);
                            } catch (Exception e) {
                                if (!topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) && !topic.equals(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC)) {
                                    this.log.warn("updateTopicRouteInfoFromNameServer Exception", (Throwable) e);
                                }
                                this.lockNamesrv.unlock();
                            }
                        }
                    }
                }
                if (topicRouteData != null) {
                    TopicRouteData old = this.topicRouteTable.get(topic);
                    boolean changed = topicRouteDataIsChange(old, topicRouteData);
                    if (!changed) {
                        changed = isNeedUpdateTopicRouteInfo(topic);
                    } else {
                        this.log.info("the topic[{}] route info changed, old[{}] ,new[{}]", topic, old, topicRouteData);
                    }
                    if (changed) {
                        TopicRouteData cloneTopicRouteData = topicRouteData.cloneTopicRouteData();
                        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                            this.brokerAddrTable.put(bd.getBrokerName(), bd.getBrokerAddrs());
                        }
                        TopicPublishInfo publishInfo = topicRouteData2TopicPublishInfo(topic, topicRouteData);
                        publishInfo.setHaveTopicRouterInfo(true);
                        for (Map.Entry<String, MQProducerInner> entry : this.producerTable.entrySet()) {
                            MQProducerInner impl = entry.getValue();
                            if (impl != null) {
                                impl.updateTopicPublishInfo(topic, publishInfo);
                            }
                        }
                        Set<MessageQueue> subscribeInfo = topicRouteData2TopicSubscribeInfo(topic, topicRouteData);
                        for (Map.Entry<String, MQConsumerInner> entry2 : this.consumerTable.entrySet()) {
                            MQConsumerInner impl2 = entry2.getValue();
                            if (impl2 != null) {
                                impl2.updateTopicSubscribeInfo(topic, subscribeInfo);
                            }
                        }
                        this.log.info("topicRouteTable.put. Topic = {}, TopicRouteData[{}]", topic, cloneTopicRouteData);
                        this.topicRouteTable.put(topic, cloneTopicRouteData);
                        this.lockNamesrv.unlock();
                        return true;
                    }
                } else {
                    this.log.warn("updateTopicRouteInfoFromNameServer, getTopicRouteInfoFromNameServer return null, Topic: {}", topic);
                }
                this.lockNamesrv.unlock();
            } else {
                this.log.warn("updateTopicRouteInfoFromNameServer tryLock timeout {}ms", Long.valueOf((long) LOCK_TIMEOUT_MILLIS));
            }
            return false;
        } catch (Exception e2) {
            this.log.warn("updateTopicRouteInfoFromNameServer Exception", (Throwable) e2);
            return false;
        }
    }

    private HeartbeatData prepareHeartbeatData() {
        HeartbeatData heartbeatData = new HeartbeatData();
        heartbeatData.setClientID(this.clientId);
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            MQConsumerInner impl = entry.getValue();
            if (impl != null) {
                ConsumerData consumerData = new ConsumerData();
                consumerData.setGroupName(impl.groupName());
                consumerData.setConsumeType(impl.consumeType());
                consumerData.setMessageModel(impl.messageModel());
                consumerData.setConsumeFromWhere(impl.consumeFromWhere());
                consumerData.getSubscriptionDataSet().addAll(impl.subscriptions());
                consumerData.setUnitMode(impl.isUnitMode());
                heartbeatData.getConsumerDataSet().add(consumerData);
            }
        }
        for (Map.Entry<String, MQProducerInner> entry2 : this.producerTable.entrySet()) {
            if (entry2.getValue() != null) {
                ProducerData producerData = new ProducerData();
                producerData.setGroupName(entry2.getKey());
                heartbeatData.getProducerDataSet().add(producerData);
            }
        }
        return heartbeatData;
    }

    private boolean isBrokerInNameServer(String brokerAddr) {
        for (Map.Entry<String, TopicRouteData> itNext : this.topicRouteTable.entrySet()) {
            for (BrokerData bd : itNext.getValue().getBrokerDatas()) {
                if (bd.getBrokerAddrs().containsValue(brokerAddr)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void uploadFilterClassToAllFilterServer(String consumerGroup, String fullClassName, String topic, String filterClassSource) throws UnsupportedEncodingException {
        byte[] classBody = null;
        int classCRC = 0;
        try {
            classBody = filterClassSource.getBytes("UTF-8");
            classCRC = UtilAll.crc32(classBody);
        } catch (Exception e1) {
            this.log.warn("uploadFilterClassToAllFilterServer Exception, ClassName: {} {}", fullClassName, RemotingHelper.exceptionSimpleDesc(e1));
        }
        TopicRouteData topicRouteData = this.topicRouteTable.get(topic);
        if (topicRouteData == null || topicRouteData.getFilterServerTable() == null || topicRouteData.getFilterServerTable().isEmpty()) {
            this.log.warn("register message class filter failed, because no filter server, ConsumerGroup: {} Topic: {} ClassName: {}", consumerGroup, topic, fullClassName);
            return;
        }
        for (Map.Entry<String, List<String>> next : topicRouteData.getFilterServerTable().entrySet()) {
            for (String fsAddr : next.getValue()) {
                try {
                    this.mQClientAPIImpl.registerMessageFilterClass(fsAddr, consumerGroup, topic, fullClassName, classCRC, classBody, FixedBackOff.DEFAULT_INTERVAL);
                    this.log.info("register message class filter to {} OK, ConsumerGroup: {} Topic: {} ClassName: {}", fsAddr, consumerGroup, topic, fullClassName);
                } catch (Exception e) {
                    this.log.error("uploadFilterClassToAllFilterServer Exception", (Throwable) e);
                }
            }
        }
    }

    private boolean topicRouteDataIsChange(TopicRouteData olddata, TopicRouteData nowdata) {
        if (olddata == null || nowdata == null) {
            return true;
        }
        TopicRouteData old = olddata.cloneTopicRouteData();
        TopicRouteData now = nowdata.cloneTopicRouteData();
        Collections.sort(old.getQueueDatas());
        Collections.sort(old.getBrokerDatas());
        Collections.sort(now.getQueueDatas());
        Collections.sort(now.getBrokerDatas());
        return !old.equals(now);
    }

    private boolean isNeedUpdateTopicRouteInfo(String topic) {
        boolean result = false;
        Iterator<Map.Entry<String, MQProducerInner>> it = this.producerTable.entrySet().iterator();
        while (it.hasNext() && !result) {
            MQProducerInner impl = it.next().getValue();
            if (impl != null) {
                result = impl.isPublishTopicNeedUpdate(topic);
            }
        }
        Iterator<Map.Entry<String, MQConsumerInner>> it2 = this.consumerTable.entrySet().iterator();
        while (it2.hasNext() && !result) {
            MQConsumerInner impl2 = it2.next().getValue();
            if (impl2 != null) {
                result = impl2.isSubscribeTopicNeedUpdate(topic);
            }
        }
        return result;
    }

    public void shutdown() {
        if (this.consumerTable.isEmpty() && this.adminExtTable.isEmpty() && this.producerTable.size() <= 1) {
            synchronized (this) {
                switch (this.serviceState) {
                    case RUNNING:
                        this.defaultMQProducer.getDefaultMQProducerImpl().shutdown(false);
                        this.serviceState = ServiceState.SHUTDOWN_ALREADY;
                        this.pullMessageService.shutdown(true);
                        this.scheduledExecutorService.shutdown();
                        this.mQClientAPIImpl.shutdown();
                        this.rebalanceService.shutdown();
                        MQClientManager.getInstance().removeClientFactory(this.clientId);
                        this.log.info("the client factory [{}] shutdown OK", this.clientId);
                        break;
                }
            }
        }
    }

    public boolean registerConsumer(String group, MQConsumerInner consumer) {
        if (null == group || null == consumer) {
            return false;
        }
        if (this.consumerTable.putIfAbsent(group, consumer) == null) {
            return true;
        }
        this.log.warn("the consumer group[" + group + "] exist already.");
        return false;
    }

    public void unregisterConsumer(String group) {
        this.consumerTable.remove(group);
        unregisterClientWithLock(null, group);
    }

    private void unregisterClientWithLock(String producerGroup, String consumerGroup) {
        try {
            if (this.lockHeartbeat.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    unregisterClient(producerGroup, consumerGroup);
                    this.lockHeartbeat.unlock();
                } catch (Exception e) {
                    this.log.error("unregisterClient exception", (Throwable) e);
                    this.lockHeartbeat.unlock();
                }
            } else {
                this.log.warn("lock heartBeat, but failed.");
            }
        } catch (InterruptedException e2) {
            this.log.warn("unregisterClientWithLock exception", (Throwable) e2);
        }
    }

    private void unregisterClient(String producerGroup, String consumerGroup) {
        for (Map.Entry<String, HashMap<Long, String>> entry : this.brokerAddrTable.entrySet()) {
            String brokerName = entry.getKey();
            HashMap<Long, String> oneTable = entry.getValue();
            if (oneTable != null) {
                for (Map.Entry<Long, String> entry1 : oneTable.entrySet()) {
                    String addr = entry1.getValue();
                    if (addr != null) {
                        try {
                            this.mQClientAPIImpl.unregisterClient(addr, this.clientId, producerGroup, consumerGroup, LOCK_TIMEOUT_MILLIS);
                            this.log.info("unregister client[Producer: {} Consumer: {}] from broker[{} {} {}] success", producerGroup, consumerGroup, brokerName, entry1.getKey(), addr);
                        } catch (MQBrokerException e) {
                            this.log.error("unregister client exception from broker: " + addr, (Throwable) e);
                        } catch (RemotingException e2) {
                            this.log.error("unregister client exception from broker: " + addr, (Throwable) e2);
                        } catch (InterruptedException e3) {
                            this.log.error("unregister client exception from broker: " + addr, (Throwable) e3);
                        }
                    }
                }
            }
        }
    }

    public boolean registerProducer(String group, DefaultMQProducerImpl producer) {
        if (null == group || null == producer) {
            return false;
        }
        if (this.producerTable.putIfAbsent(group, producer) == null) {
            return true;
        }
        this.log.warn("the producer group[{}] exist already.", group);
        return false;
    }

    public void unregisterProducer(String group) {
        this.producerTable.remove(group);
        unregisterClientWithLock(group, null);
    }

    public boolean registerAdminExt(String group, MQAdminExtInner admin) {
        if (null == group || null == admin) {
            return false;
        }
        if (this.adminExtTable.putIfAbsent(group, admin) == null) {
            return true;
        }
        this.log.warn("the admin group[{}] exist already.", group);
        return false;
    }

    public void unregisterAdminExt(String group) {
        this.adminExtTable.remove(group);
    }

    public void rebalanceImmediately() {
        this.rebalanceService.wakeup();
    }

    public void doRebalance() {
        for (Map.Entry<String, MQConsumerInner> entry : this.consumerTable.entrySet()) {
            MQConsumerInner impl = entry.getValue();
            if (impl != null) {
                try {
                    impl.doRebalance();
                } catch (Throwable e) {
                    this.log.error("doRebalance exception", e);
                }
            }
        }
    }

    public MQProducerInner selectProducer(String group) {
        return this.producerTable.get(group);
    }

    public MQConsumerInner selectConsumer(String group) {
        return this.consumerTable.get(group);
    }

    public FindBrokerResult findBrokerAddressInAdmin(String brokerName) {
        String brokerAddr = null;
        boolean slave = false;
        boolean found = false;
        HashMap<Long, String> map = this.brokerAddrTable.get(brokerName);
        if (map != null && !map.isEmpty()) {
            Iterator<Map.Entry<Long, String>> it = map.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<Long, String> entry = it.next();
                Long id = entry.getKey();
                brokerAddr = entry.getValue();
                if (brokerAddr != null) {
                    found = true;
                    slave = 0 != id.longValue();
                }
            }
        }
        if (found) {
            return new FindBrokerResult(brokerAddr, slave, findBrokerVersion(brokerName, brokerAddr));
        }
        return null;
    }

    public String findBrokerAddressInPublish(String brokerName) {
        HashMap<Long, String> map = this.brokerAddrTable.get(brokerName);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map.get(0L);
    }

    public FindBrokerResult findBrokerAddressInSubscribe(String brokerName, long brokerId, boolean onlyThisBroker) {
        String brokerAddr = null;
        boolean slave = false;
        boolean found = false;
        HashMap<Long, String> map = this.brokerAddrTable.get(brokerName);
        if (map != null && !map.isEmpty()) {
            brokerAddr = map.get(Long.valueOf(brokerId));
            slave = brokerId != 0;
            found = brokerAddr != null;
            if (!found && !onlyThisBroker) {
                Map.Entry<Long, String> entry = map.entrySet().iterator().next();
                brokerAddr = entry.getValue();
                slave = entry.getKey().longValue() != 0;
                found = true;
            }
        }
        if (found) {
            return new FindBrokerResult(brokerAddr, slave, findBrokerVersion(brokerName, brokerAddr));
        }
        return null;
    }

    public int findBrokerVersion(String brokerName, String brokerAddr) {
        if (!this.brokerVersionTable.containsKey(brokerName) || !this.brokerVersionTable.get(brokerName).containsKey(brokerAddr)) {
            return 0;
        }
        return this.brokerVersionTable.get(brokerName).get(brokerAddr).intValue();
    }

    public List<String> findConsumerIdList(String topic, String group) {
        String brokerAddr = findBrokerAddrByTopic(topic);
        if (null == brokerAddr) {
            updateTopicRouteInfoFromNameServer(topic);
            brokerAddr = findBrokerAddrByTopic(topic);
        }
        if (null == brokerAddr) {
            return null;
        }
        try {
            return this.mQClientAPIImpl.getConsumerIdListByGroup(brokerAddr, group, LOCK_TIMEOUT_MILLIS);
        } catch (Exception e) {
            this.log.warn("getConsumerIdListByGroup exception, " + brokerAddr + " " + group, (Throwable) e);
            return null;
        }
    }

    public String findBrokerAddrByTopic(String topic) {
        TopicRouteData topicRouteData = this.topicRouteTable.get(topic);
        if (topicRouteData == null) {
            return null;
        }
        List<BrokerData> brokers = topicRouteData.getBrokerDatas();
        if (!brokers.isEmpty()) {
            return brokers.get(this.random.nextInt(brokers.size()) % brokers.size()).selectBrokerAddr();
        }
        return null;
    }

    public void resetOffset(String topic, String group, Map<MessageQueue, Long> offsetTable) {
        DefaultMQPushConsumerImpl consumer = null;

        try {
            MQConsumerInner impl = (MQConsumerInner)this.consumerTable.get(group);
            if (impl != null && impl instanceof DefaultMQPushConsumerImpl) {
                consumer = (DefaultMQPushConsumerImpl)impl;
                consumer.suspend();
                ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = consumer.getRebalanceImpl().getProcessQueueTable();
                Iterator iterator = processQueueTable.entrySet().iterator();

                while(iterator.hasNext()) {
                    Map.Entry<MessageQueue, ProcessQueue> entry = (Map.Entry)iterator.next();
                    MessageQueue mq = (MessageQueue)entry.getKey();
                    if (topic.equals(mq.getTopic()) && offsetTable.containsKey(mq)) {
                        ProcessQueue pq = (ProcessQueue)entry.getValue();
                        pq.setDropped(true);
                        pq.clear();
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(10L);
                } catch (InterruptedException var16) {
                }

                iterator = processQueueTable.keySet().iterator();

                while(iterator.hasNext()) {
                    MessageQueue mq = (MessageQueue)iterator.next();
                    Long offset = (Long)offsetTable.get(mq);
                    if (topic.equals(mq.getTopic()) && offset != null) {
                        try {
                            consumer.updateConsumeOffset(mq, offset);
                            consumer.getRebalanceImpl().removeUnnecessaryMessageQueue(mq, (ProcessQueue)processQueueTable.get(mq));
                            iterator.remove();
                        } catch (Exception var15) {
                            this.log.warn("reset offset failed. group={}, {}", new Object[]{group, mq, var15});
                        }
                    }
                }

                return;
            }

            this.log.info("[reset-offset] consumer dose not exist. group={}", group);
        } finally {
            if (consumer != null) {
                consumer.resume();
            }

        }

    }

    public Map<MessageQueue, Long> getConsumerStatus(String topic, String group) {
        MQConsumerInner impl = this.consumerTable.get(group);
        if (impl != null && (impl instanceof DefaultMQPushConsumerImpl)) {
            return ((DefaultMQPushConsumerImpl) impl).getOffsetStore().cloneOffsetTable(topic);
        }
        if (impl == null || !(impl instanceof DefaultMQPullConsumerImpl)) {
            return Collections.EMPTY_MAP;
        }
        return ((DefaultMQPullConsumerImpl) impl).getOffsetStore().cloneOffsetTable(topic);
    }

    public TopicRouteData getAnExistTopicRouteData(String topic) {
        return this.topicRouteTable.get(topic);
    }

    public MQClientAPIImpl getMQClientAPIImpl() {
        return this.mQClientAPIImpl;
    }

    public MQAdminImpl getMQAdminImpl() {
        return this.mQAdminImpl;
    }

    public long getBootTimestamp() {
        return this.bootTimestamp;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return this.scheduledExecutorService;
    }

    public PullMessageService getPullMessageService() {
        return this.pullMessageService;
    }

    public DefaultMQProducer getDefaultMQProducer() {
        return this.defaultMQProducer;
    }

    public ConcurrentMap<String, TopicRouteData> getTopicRouteTable() {
        return this.topicRouteTable;
    }

    public ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt msg, String consumerGroup, String brokerName) {
        MQConsumerInner mqConsumerInner = this.consumerTable.get(consumerGroup);
        if (null != mqConsumerInner) {
            return ((DefaultMQPushConsumerImpl) mqConsumerInner).getConsumeMessageService().consumeMessageDirectly(msg, brokerName);
        }
        return null;
    }

    public ConsumerRunningInfo consumerRunningInfo(String consumerGroup) {
        MQConsumerInner mqConsumerInner = this.consumerTable.get(consumerGroup);
        ConsumerRunningInfo consumerRunningInfo = mqConsumerInner.consumerRunningInfo();
        List<String> nsList = this.mQClientAPIImpl.getRemotingClient().getNameServerAddressList();
        StringBuilder strBuilder = new StringBuilder();
        if (nsList != null) {
            for (String addr : nsList) {
                strBuilder.append(addr).append(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
            }
        }
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_NAMESERVER_ADDR, strBuilder.toString());
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_CONSUME_TYPE, mqConsumerInner.consumeType().name());
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_CLIENT_VERSION, MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
        return consumerRunningInfo;
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return this.consumerStatsManager;
    }

    public NettyClientConfig getNettyClientConfig() {
        return this.nettyClientConfig;
    }

    public ClientConfig getClientConfig() {
        return this.clientConfig;
    }
}
