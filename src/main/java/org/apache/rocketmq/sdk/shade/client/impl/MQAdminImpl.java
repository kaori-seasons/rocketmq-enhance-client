package org.apache.rocketmq.sdk.shade.client.impl;

import org.apache.rocketmq.sdk.shade.client.QueryResult;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.TopicConfig;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageId;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryMessageResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.route.BrokerData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingUtil;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.shade.remoting.netty.ResponseFuture;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MQAdminImpl {
    private final MQClientInstance mQClientFactory;
    private final InternalLogger log = ClientLogger.getLog();
    private long timeoutMillis = 6000;

    public MQAdminImpl(MQClientInstance mQClientFactory) {
        this.mQClientFactory = mQClientFactory;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        try {
            TopicRouteData topicRouteData = this.mQClientFactory.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(key, this.timeoutMillis);
            List<BrokerData> brokerDataList = topicRouteData.getBrokerDatas();
            if (brokerDataList != null && !brokerDataList.isEmpty()) {
                Collections.sort(brokerDataList);
                boolean createOKAtLeastOnce = false;
                MQClientException exception = null;

                for (BrokerData brokerData : brokerDataList) {
                    String addr = (String)brokerData.getBrokerAddrs().get(Long.valueOf(0L));
                    if (addr != null) {
                        TopicConfig topicConfig = new TopicConfig(newTopic);
                        topicConfig.setReadQueueNums(queueNum);
                        topicConfig.setWriteQueueNums(queueNum);
                        topicConfig.setTopicSysFlag(topicSysFlag);

                        boolean createOK = false;
                        for (int i = 0; i < 5; i++) {
                            try {
                                this.mQClientFactory.getMQClientAPIImpl().createTopic(addr, key, topicConfig, this.timeoutMillis);
                                createOK = true;
                                createOKAtLeastOnce = true;
                                break;
                            } catch (Exception e) {
                                if (4 == i) {
                                    exception = new MQClientException("create topic to broker exception", e);
                                }
                            }
                        }
                    }
                }

                if (exception != null && !createOKAtLeastOnce) {
                    throw exception;
                }
            } else {
                throw new MQClientException("Not found broker, maybe key is wrong", null);
            }
        } catch (Exception e) {
            throw new MQClientException("create new topic failed", e);
        }
    }

    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException {
        TopicPublishInfo topicPublishInfo;
        try {
            TopicRouteData topicRouteData = this.mQClientFactory.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(topic, this.timeoutMillis);
            if (topicRouteData != null && (topicPublishInfo = MQClientInstance.topicRouteData2TopicPublishInfo(topic, topicRouteData)) != null && topicPublishInfo.ok()) {
                return topicPublishInfo.getMessageQueueList();
            }
            throw new MQClientException("Unknow why, Can not find Message Queue for this topic, " + topic, (Throwable) null);
        } catch (Exception e) {
            throw new MQClientException("Can not find Message Queue for this topic, " + topic, e);
        }
    }

    public List<MessageQueue> parsePublishMessageQueues(List<MessageQueue> messageQueueList) {
        List<MessageQueue> resultQueues = new ArrayList<>();
        for (MessageQueue queue : messageQueueList) {
            resultQueues.add(new MessageQueue(NamespaceUtil.withoutNamespace(queue.getTopic(), this.mQClientFactory.getClientConfig().getNamespace()), queue.getBrokerName(), queue.getQueueId()));
        }
        return resultQueues;
    }

    public Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException {
        try {
            TopicRouteData topicRouteData = this.mQClientFactory.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(topic, this.timeoutMillis);
            if (topicRouteData != null) {
                Set<MessageQueue> mqList = MQClientInstance.topicRouteData2TopicSubscribeInfo(topic, topicRouteData);
                if (!mqList.isEmpty()) {
                    return mqList;
                }
                throw new MQClientException("Can not find Message Queue for this topic, " + topic + " Namesrv return empty", (Throwable) null);
            }
            throw new MQClientException("Unknow why, Can not find Message Queue for this topic, " + topic, (Throwable) null);
        } catch (Exception e) {
            throw new MQClientException("Can not find Message Queue for this topic, " + topic + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), e);
        }
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }
        if (brokerAddr != null) {
            try {
                return this.mQClientFactory.getMQClientAPIImpl().searchOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), timestamp, this.timeoutMillis);
            } catch (Exception e) {
                throw new MQClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        }
    }

    public long maxOffset(MessageQueue mq) throws MQClientException {
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }
        if (brokerAddr != null) {
            try {
                return this.mQClientFactory.getMQClientAPIImpl().getMaxOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), this.timeoutMillis);
            } catch (Exception e) {
                throw new MQClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        }
    }

    public long minOffset(MessageQueue mq) throws MQClientException {
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }
        if (brokerAddr != null) {
            try {
                return this.mQClientFactory.getMQClientAPIImpl().getMinOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), this.timeoutMillis);
            } catch (Exception e) {
                throw new MQClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        }
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }
        if (brokerAddr != null) {
            try {
                return this.mQClientFactory.getMQClientAPIImpl().getEarliestMsgStoretime(brokerAddr, mq.getTopic(), mq.getQueueId(), this.timeoutMillis);
            } catch (Exception e) {
                throw new MQClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        }
    }

    public MessageExt viewMessage(String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        try {
            MessageId messageId = MessageDecoder.decodeMessageId(msgId);
            return this.mQClientFactory.getMQClientAPIImpl().viewMessage(RemotingUtil.socketAddress2String(messageId.getAddress()), messageId.getOffset(), this.timeoutMillis);
        } catch (Exception e) {
            throw new MQClientException(208, "query message by id finished, but no message.");
        }
    }

    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        return queryMessage(topic, key, maxNum, begin, end, false);
    }

    public MessageExt queryMessageByUniqKey(String topic, String uniqKey) throws InterruptedException, MQClientException {
        QueryResult qr = queryMessage(topic, uniqKey, 32, MessageClientIDSetter.getNearlyTimeFromID(uniqKey).getTime() - 1000, Long.MAX_VALUE, true);
        if (qr == null || qr.getMessageList() == null || qr.getMessageList().size() <= 0) {
            return null;
        }
        return qr.getMessageList().get(0);
    }

    protected QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end, boolean isUniqKey) throws MQClientException, InterruptedException {
        TopicRouteData topicRouteData = this.mQClientFactory.getAnExistTopicRouteData(topic);
        if (null == topicRouteData) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicRouteData = this.mQClientFactory.getAnExistTopicRouteData(topic);
        }
        if (topicRouteData != null) {
            List<String> brokerAddrs = new LinkedList<>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                String addr = brokerData.selectBrokerAddr();
                if (addr != null) {
                    brokerAddrs.add(addr);
                }
            }
            if (!brokerAddrs.isEmpty()) {
                final CountDownLatch countDownLatch = new CountDownLatch(brokerAddrs.size());
                final List<QueryResult> queryResultList = new LinkedList<>();
                final ReadWriteLock lock = new ReentrantReadWriteLock(false);
                for (String addr2 : brokerAddrs) {
                    try {
                        QueryMessageRequestHeader requestHeader = new QueryMessageRequestHeader();
                        requestHeader.setTopic(topic);
                        requestHeader.setKey(key);
                        requestHeader.setMaxNum(Integer.valueOf(maxNum));
                        requestHeader.setBeginTimestamp(Long.valueOf(begin));
                        requestHeader.setEndTimestamp(Long.valueOf(end));
                        this.mQClientFactory.getMQClientAPIImpl().queryMessage(addr2, requestHeader, this.timeoutMillis * 3, new InvokeCallback() {
                            @Override
                            public void operationComplete(ResponseFuture responseFuture)  {
                                try {
                                    RemotingCommand response = responseFuture.getResponseCommand();
                                    if (response != null) {
                                        QueryMessageResponseHeader responseHeader; List<MessageExt> wrappers; QueryResult qr; switch (response.getCode()) {
                                            case 0:
                                                responseHeader = null;


                                                try {
                                                    responseHeader = (QueryMessageResponseHeader)response.decodeCommandCustomHeader(QueryMessageResponseHeader.class);
                                                } catch (RemotingCommandException e) {
                                                    MQAdminImpl.this.log.error("decodeCommandCustomHeader exception", (Throwable)e);

                                                    return;
                                                }

                                                wrappers = MessageDecoder.decodes(ByteBuffer.wrap(response.getBody()), true);

                                                qr = new QueryResult(responseHeader.getIndexLastUpdateTimestamp().longValue(), wrappers);
                                                try {
                                                    lock.writeLock().lock();
                                                    queryResultList.add(qr);
                                                } finally {
                                                    lock.writeLock().unlock();
                                                }
                                                break;

                                            default:
                                                MQAdminImpl.this.log.warn("getResponseCommand failed, {} {}", Integer.valueOf(response.getCode()), response.getRemark());
                                                break;
                                        }
                                    } else {
                                        MQAdminImpl.this.log.warn("getResponseCommand return null");
                                    }
                                } finally {
                                    countDownLatch.countDown();
                                }
                            }
                        }, Boolean.valueOf(isUniqKey));
                    } catch (Exception e) {
                        this.log.warn("queryMessage exception", (Throwable) e);
                    }
                }
                if (!countDownLatch.await(this.timeoutMillis * 4, TimeUnit.MILLISECONDS)) {
                    this.log.warn("queryMessage, maybe some broker failed");
                }
                long c = 0L;
                List<MessageExt> messageList = new LinkedList<>();
                for (QueryResult qr : queryResultList) {
                    if (qr.getIndexLastUpdateTimestamp() > c) {
                        c = qr.getIndexLastUpdateTimestamp();
                    }
                    for (MessageExt msgExt : qr.getMessageList()) {
                        if (!isUniqKey) {
                            String keys = msgExt.getKeys();
                            if (keys != null) {
                                boolean matched = false;
                                String[] keyArray = keys.split(" ");
                                if (keyArray != null) {
                                    int length = keyArray.length;
                                    int i = 0;
                                    while (true) {
                                        if (i >= length) {
                                            break;
                                        } else if (key.equals(keyArray[i])) {
                                            matched = true;
                                            break;
                                        } else {
                                            i++;
                                        }
                                    }
                                }
                                if (matched) {
                                    messageList.add(msgExt);
                                } else {
                                    this.log.warn("queryMessage, find message key not matched, maybe hash duplicate {}", msgExt.toString());
                                }
                            }
                        } else if (!msgExt.getMsgId().equals(key)) {
                            this.log.warn("queryMessage by uniqKey, find message key not matched, maybe hash duplicate {}", msgExt.toString());
                        } else if (messageList.size() <= 0) {
                            messageList.add(msgExt);
                        } else if (messageList.get(0).getStoreTimestamp() > msgExt.getStoreTimestamp()) {
                            messageList.clear();
                            messageList.add(msgExt);
                        }
                    }
                }
                for (MessageExt messageExt : messageList) {
                    if (null != this.mQClientFactory.getClientConfig().getNamespace()) {
                        messageExt.setTopic(NamespaceUtil.withoutNamespace(messageExt.getTopic(), this.mQClientFactory.getClientConfig().getNamespace()));
                    }
                }
                if (!messageList.isEmpty()) {
                    return new QueryResult(c, messageList);
                }
                throw new MQClientException(208, "query message by key finished, but no message.");
            }
        }
        throw new MQClientException(17, "The topic[" + topic + "] not matched route info");
    }
}
