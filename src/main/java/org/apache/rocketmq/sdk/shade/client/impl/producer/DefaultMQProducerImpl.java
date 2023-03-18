package org.apache.rocketmq.sdk.shade.client.impl.producer;

import org.apache.rocketmq.sdk.shade.client.QueryResult;
import org.apache.rocketmq.sdk.shade.client.Validators;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.hook.CheckForbiddenContext;
import org.apache.rocketmq.sdk.shade.client.hook.CheckForbiddenHook;
import org.apache.rocketmq.sdk.shade.client.hook.SendMessageContext;
import org.apache.rocketmq.sdk.shade.client.hook.SendMessageHook;
import org.apache.rocketmq.sdk.shade.client.impl.CommunicationMode;
import org.apache.rocketmq.sdk.shade.client.impl.MQClientManager;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.latency.MQFaultStrategy;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionState;
import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.client.producer.SendCallback;
import org.apache.rocketmq.sdk.shade.client.producer.SendResult;
import org.apache.rocketmq.sdk.shade.client.producer.SendStatus;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionCheckListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionSendResult;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.ServiceState;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.MessageBatch;
import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageId;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.message.MessageType;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.header.CheckTransactionStateRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.EndTransactionRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultMQProducerImpl implements MQProducerInner {
    private final InternalLogger log;
    private final Random random;
    private final DefaultMQProducer defaultMQProducer;
    private final ConcurrentMap<String, TopicPublishInfo> topicPublishInfoTable;
    private final ArrayList<SendMessageHook> sendMessageHookList;
    private final RPCHook rpcHook;
    protected BlockingQueue<Runnable> checkRequestQueue;
    protected ExecutorService checkExecutor;
    private ServiceState serviceState;
    private MQClientInstance mQClientFactory;
    private ArrayList<CheckForbiddenHook> checkForbiddenHookList;
    private int zipCompressLevel;
    private MQFaultStrategy mqFaultStrategy;

    public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer) {
        this(defaultMQProducer, (RPCHook)null);
    }

    public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer, RPCHook rpcHook) {
        this.log = ClientLogger.getLog();
        this.random = new Random();
        this.topicPublishInfoTable = new ConcurrentHashMap();
        this.sendMessageHookList = new ArrayList();
        this.serviceState = ServiceState.CREATE_JUST;
        this.checkForbiddenHookList = new ArrayList();
        this.zipCompressLevel = Integer.parseInt(System.getProperty("rocketmq.message.compressLevel", "5"));
        this.mqFaultStrategy = new MQFaultStrategy();
        this.defaultMQProducer = defaultMQProducer;
        this.rpcHook = rpcHook;
    }

    public void registerCheckForbiddenHook(CheckForbiddenHook checkForbiddenHook) {
        this.checkForbiddenHookList.add(checkForbiddenHook);
        this.log.info("register a new checkForbiddenHook. hookName={}, allHookSize={}", checkForbiddenHook.hookName(), this.checkForbiddenHookList.size());
    }

    public void initTransactionEnv() {
        TransactionMQProducer producer = (TransactionMQProducer)this.defaultMQProducer;
        if (producer.getExecutorService() != null) {
            this.checkExecutor = producer.getExecutorService();
        } else {
            this.checkRequestQueue = new LinkedBlockingQueue(producer.getCheckRequestHoldMax());
            this.checkExecutor = new ThreadPoolExecutor(producer.getCheckThreadPoolMinSize(), producer.getCheckThreadPoolMaxSize(), 60000L, TimeUnit.MILLISECONDS, this.checkRequestQueue);
        }

    }

    public void destroyTransactionEnv() {
        if (this.checkExecutor != null) {
            this.checkExecutor.shutdown();
        }

    }

    public void registerSendMessageHook(SendMessageHook hook) {
        this.sendMessageHookList.add(hook);
        this.log.info("register sendMessage Hook, {}", hook.hookName());
    }

    public void start() throws MQClientException {
        this.start(true);
    }

    public void start(boolean startFactory) throws MQClientException {
        switch(this.serviceState) {
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;
                this.checkConfig();
                if (!this.defaultMQProducer.getProducerGroup().equals("CLIENT_INNER_PRODUCER")) {
                    this.defaultMQProducer.changeInstanceNameToPID();
                }

                this.mQClientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultMQProducer, this.rpcHook);
                boolean registerOK = this.mQClientFactory.registerProducer(this.defaultMQProducer.getProducerGroup(), this);
                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;
                    throw new MQClientException("The producer group[" + this.defaultMQProducer.getProducerGroup() + "] has been created before, specify another name please." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
                } else {
                    this.topicPublishInfoTable.put(this.defaultMQProducer.getCreateTopicKey(), new TopicPublishInfo());
                    if (startFactory) {
                        this.mQClientFactory.start();
                    }

                    this.log.info("the producer [{}] start OK. sendMessageWithVIPChannel={}", this.defaultMQProducer.getProducerGroup(), this.defaultMQProducer.isSendMessageWithVIPChannel());
                    this.serviceState = ServiceState.RUNNING;
                }
            default:
                this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
                return;
            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new MQClientException("The producer service state not OK, maybe started once, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    private void checkConfig() throws MQClientException {
        Validators.checkGroup(this.defaultMQProducer.getProducerGroup());
        if (null == this.defaultMQProducer.getProducerGroup()) {
            throw new MQClientException("producerGroup is null", (Throwable)null);
        } else if (this.defaultMQProducer.getProducerGroup().equals("DEFAULT_PRODUCER")) {
            throw new MQClientException("producerGroup can not equal DEFAULT_PRODUCER, please specify another one.", (Throwable)null);
        }
    }

    public void shutdown() {
        this.shutdown(true);
    }

    public void shutdown(boolean shutdownFactory) {
        switch(this.serviceState) {
            case RUNNING:
                this.mQClientFactory.unregisterProducer(this.defaultMQProducer.getProducerGroup());
                if (shutdownFactory) {
                    this.mQClientFactory.shutdown();
                }

                this.log.info("the producer [{}] shutdown OK", this.defaultMQProducer.getProducerGroup());
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
            case CREATE_JUST:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
            default:
        }
    }

    public Set<String> getPublishTopicList() {
        Set<String> topicList = new HashSet();
        Iterator var2 = this.topicPublishInfoTable.keySet().iterator();

        while(var2.hasNext()) {
            String key = (String)var2.next();
            topicList.add(key);
        }

        return topicList;
    }

    public boolean isPublishTopicNeedUpdate(String topic) {
        TopicPublishInfo prev = (TopicPublishInfo)this.topicPublishInfoTable.get(topic);
        return null == prev || !prev.ok();
    }

    /** @deprecated */
    @Deprecated
    public TransactionCheckListener checkListener() {
        if (this.defaultMQProducer instanceof TransactionMQProducer) {
            TransactionMQProducer producer = (TransactionMQProducer)this.defaultMQProducer;
            return producer.getTransactionCheckListener();
        } else {
            return null;
        }
    }

    public TransactionListener getCheckListener() {
        if (this.defaultMQProducer instanceof TransactionMQProducer) {
            TransactionMQProducer producer = (TransactionMQProducer)this.defaultMQProducer;
            return producer.getTransactionListener();
        } else {
            return null;
        }
    }

    public void checkTransactionState(final String addr, final MessageExt msg, final CheckTransactionStateRequestHeader header) {
        Runnable request = new Runnable() {
            private final String brokerAddr = addr;
            private final MessageExt message = msg;
            private final CheckTransactionStateRequestHeader checkRequestHeader = header;
            private final String group;

            {
                this.group = DefaultMQProducerImpl.this.defaultMQProducer.getProducerGroup();
            }

            public void run() {
                TransactionCheckListener transactionCheckListener = DefaultMQProducerImpl.this.checkListener();
                TransactionListener transactionListener = DefaultMQProducerImpl.this.getCheckListener();
                if (transactionCheckListener == null && transactionListener == null) {
                    DefaultMQProducerImpl.this.log.warn("CheckTransactionState, pick transactionCheckListener by group[{}] failed", this.group);
                } else {
                    LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
                    Throwable exception = null;

                    try {
                        if (transactionCheckListener != null) {
                            localTransactionState = transactionCheckListener.checkLocalTransactionState(this.message);
                        } else if (transactionListener != null) {
                            DefaultMQProducerImpl.this.log.debug("Used new check API in transaction message");
                            localTransactionState = transactionListener.checkLocalTransaction(this.message);
                        }
                    } catch (Throwable var6) {
                        DefaultMQProducerImpl.this.log.error("Broker call checkTransactionState, but checkLocalTransactionState exception", var6);
                        exception = var6;
                    }

                    this.processTransactionState(localTransactionState, this.group, exception);
                }

            }

            private void processTransactionState(LocalTransactionState localTransactionState, String producerGroup, Throwable exception) {
                EndTransactionRequestHeader thisHeader = new EndTransactionRequestHeader();
                thisHeader.setCommitLogOffset(this.checkRequestHeader.getCommitLogOffset());
                thisHeader.setProducerGroup(producerGroup);
                thisHeader.setTranStateTableOffset(this.checkRequestHeader.getTranStateTableOffset());
                thisHeader.setFromTransactionCheck(true);
                String uniqueKey = (String)this.message.getProperties().get("UNIQ_KEY");
                if (uniqueKey == null) {
                    uniqueKey = this.message.getMsgId();
                }

                thisHeader.setMsgId(uniqueKey);
                thisHeader.setTransactionId(this.checkRequestHeader.getTransactionId());
                switch(localTransactionState) {
                    case COMMIT_MESSAGE:
                        thisHeader.setCommitOrRollback(8);
                        break;
                    case ROLLBACK_MESSAGE:
                        thisHeader.setCommitOrRollback(12);
                        DefaultMQProducerImpl.this.log.warn("when broker check, client rollback this transaction, {}", thisHeader);
                        break;
                    case UNKNOW:
                        thisHeader.setCommitOrRollback(0);
                        DefaultMQProducerImpl.this.log.warn("when broker check, client does not know this transaction state, {}", thisHeader);
                }

                String remark = null;
                if (exception != null) {
                    remark = "checkLocalTransactionState Exception: " + RemotingHelper.exceptionSimpleDesc(exception);
                }

                try {
                    DefaultMQProducerImpl.this.mQClientFactory.getMQClientAPIImpl().endTransactionOneway(this.brokerAddr, thisHeader, remark, 3000L);
                } catch (Exception var8) {
                    DefaultMQProducerImpl.this.log.error("endTransactionOneway exception", var8);
                }

            }
        };
        this.checkExecutor.submit(request);
    }

    public void updateTopicPublishInfo(String topic, TopicPublishInfo info) {
        if (info != null && topic != null) {
            TopicPublishInfo prev = (TopicPublishInfo)this.topicPublishInfoTable.put(topic, info);
            if (prev != null) {
                this.log.info("updateTopicPublishInfo prev is not null, " + prev.toString());
            }
        }

    }

    public boolean isUnitMode() {
        return this.defaultMQProducer.isUnitMode();
    }

    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        this.createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        this.makeSureStateOK();
        Validators.checkTopic(newTopic);
        this.mQClientFactory.getMQAdminImpl().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    private void makeSureStateOK() throws MQClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new MQClientException("The producer service state not OK, " + this.serviceState + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null);
        }
    }

    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().fetchPublishMessageQueues(topic);
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().searchOffset(mq, timestamp);
    }

    public long maxOffset(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
    }

    public long minOffset(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().minOffset(mq);
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().earliestMsgStoreTime(mq);
    }

    public MessageExt viewMessage(String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().viewMessage(msgId);
    }

    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().queryMessage(topic, key, maxNum, begin, end);
    }

    public MessageExt queryMessageByUniqKey(String topic, String uniqKey) throws MQClientException, InterruptedException {
        this.makeSureStateOK();
        return this.mQClientFactory.getMQAdminImpl().queryMessageByUniqKey(topic, uniqKey);
    }

    public void send(Message msg, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        this.send(msg, sendCallback, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    /** @deprecated */
    @Deprecated
    public void send(final Message msg, final SendCallback sendCallback, final long timeout) throws MQClientException, RemotingException, InterruptedException {
        final long beginStartTime = System.currentTimeMillis();
        ExecutorService executor = this.getCallbackExecutor();

        try {
            executor.submit(new Runnable() {
                public void run() {
                    long costTime = System.currentTimeMillis() - beginStartTime;
                    if (timeout > costTime) {
                        try {
                            DefaultMQProducerImpl.this.sendDefaultImpl(msg, CommunicationMode.ASYNC, sendCallback, timeout - costTime);
                        } catch (Exception var4) {
                            sendCallback.onException(var4);
                        }
                    } else {
                        sendCallback.onException(new RemotingTooMuchRequestException("DEFAULT ASYNC send call timeout"));
                    }

                }
            });
        } catch (RejectedExecutionException var9) {
            throw new MQClientException("executor rejected ", var9);
        }
    }

    public MessageQueue selectOneMessageQueue(TopicPublishInfo tpInfo, String lastBrokerName) {
        return this.mqFaultStrategy.selectOneMessageQueue(tpInfo, lastBrokerName);
    }

    public void updateFaultItem(String brokerName, long currentLatency, boolean isolation) {
        this.mqFaultStrategy.updateFaultItem(brokerName, currentLatency, isolation);
    }

    private SendResult sendDefaultImpl(Message msg, CommunicationMode communicationMode, SendCallback sendCallback, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);
        long invokeID = this.random.nextLong();
        long beginTimestampFirst = System.currentTimeMillis();
        long beginTimestampPrev = beginTimestampFirst;
        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            boolean callTimeout = false;
            MessageQueue mq = null;
            Exception exception = null;
            SendResult sendResult = null;
            int timesTotal = communicationMode == CommunicationMode.SYNC ? 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed() : 1;
            int times = 0;
            String[] brokersSent = new String[timesTotal];

            while(true) {
                label129: {
                    String info;
                    if (times < timesTotal) {
                        info = null == mq ? null : mq.getBrokerName();
                        MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, info);
                        if (mqSelected != null) {
                            mq = mqSelected;
                            brokersSent[times] = mqSelected.getBrokerName();

                            long endTimestamp;
                            try {
                                beginTimestampPrev = System.currentTimeMillis();
                                if (times > 0) {
                                    msg.setTopic(this.defaultMQProducer.withNamespace(msg.getTopic()));
                                }

                                long costTime = beginTimestampPrev - beginTimestampFirst;
                                if (timeout >= costTime) {
                                    sendResult = this.sendKernelImpl(msg, mq, communicationMode, sendCallback, topicPublishInfo, timeout - costTime);
                                    endTimestamp = System.currentTimeMillis();
                                    this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                                    switch(communicationMode) {
                                        case ASYNC:
                                            return null;
                                        case ONEWAY:
                                            return null;
                                        case SYNC:
                                            if (sendResult.getSendStatus() == SendStatus.SEND_OK || !this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                                                return sendResult;
                                            }
                                        default:
                                            break label129;
                                    }
                                }

                                callTimeout = true;
                            } catch (RemotingException var26) {
                                endTimestamp = System.currentTimeMillis();
                                this.updateFaultItem(mqSelected.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                                this.log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mqSelected), var26);
                                this.log.warn(msg.toString());
                                exception = var26;
                                break label129;
                            } catch (MQClientException var27) {
                                endTimestamp = System.currentTimeMillis();
                                this.updateFaultItem(mqSelected.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                                this.log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mqSelected), var27);
                                this.log.warn(msg.toString());
                                exception = var27;
                                break label129;
                            } catch (MQBrokerException var28) {
                                endTimestamp = System.currentTimeMillis();
                                this.updateFaultItem(mqSelected.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                                this.log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mqSelected), var28);
                                this.log.warn(msg.toString());
                                exception = var28;
                                switch(var28.getResponseCode()) {
                                    case 1:
                                    case 14:
                                    case 16:
                                    case 17:
                                    case 204:
                                    case 205:
                                        break label129;
                                    default:
                                        if (sendResult != null) {
                                            return sendResult;
                                        } else {
                                            throw var28;
                                        }
                                }
                            } catch (InterruptedException var29) {
                                endTimestamp = System.currentTimeMillis();
                                this.updateFaultItem(mqSelected.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                                this.log.warn(String.format("sendKernelImpl exception, throw exception, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mqSelected), var29);
                                this.log.warn(msg.toString());
                                this.log.warn("sendKernelImpl exception", var29);
                                this.log.warn(msg.toString());
                                throw var29;
                            }
                        }
                    }

                    if (sendResult != null) {
                        return sendResult;
                    }

                    info = String.format("Send [%d] times, still failed, cost [%d]ms, Topic: %s, BrokersSent: %s", times, System.currentTimeMillis() - beginTimestampFirst, msg.getTopic(), Arrays.toString(brokersSent));
                    info = info + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/");
                    MQClientException mqClientException = new MQClientException(info, (Throwable)exception);
                    if (callTimeout) {
                        throw new RemotingTooMuchRequestException("sendDefaultImpl call timeout");
                    }

                    if (exception instanceof MQBrokerException) {
                        mqClientException.setResponseCode(((MQBrokerException)exception).getResponseCode());
                    } else if (exception instanceof RemotingConnectException) {
                        mqClientException.setResponseCode(10001);
                    } else if (exception instanceof RemotingTimeoutException) {
                        mqClientException.setResponseCode(10002);
                    } else if (exception instanceof MQClientException) {
                        mqClientException.setResponseCode(10003);
                    }

                    throw mqClientException;
                }

                ++times;
            }
        } else {
            List<String> nsList = this.getmQClientFactory().getMQClientAPIImpl().getNameServerAddressList();
            if (null != nsList && !nsList.isEmpty()) {
                throw (new MQClientException("No route info of this topic, " + msg.getTopic() + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null)).setResponseCode(10005);
            } else {
                throw (new MQClientException("No name server address, please set it." + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), (Throwable)null)).setResponseCode(10004);
            }
        }
    }

    private TopicPublishInfo tryToFindTopicPublishInfo(String topic) {
        TopicPublishInfo topicPublishInfo = (TopicPublishInfo)this.topicPublishInfoTable.get(topic);
        if (null == topicPublishInfo || !topicPublishInfo.ok()) {
            this.topicPublishInfoTable.putIfAbsent(topic, new TopicPublishInfo());
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicPublishInfo = (TopicPublishInfo)this.topicPublishInfoTable.get(topic);
        }

        if (!topicPublishInfo.isHaveTopicRouterInfo() && !topicPublishInfo.ok()) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic, true, this.defaultMQProducer);
            topicPublishInfo = (TopicPublishInfo)this.topicPublishInfoTable.get(topic);
            return topicPublishInfo;
        } else {
            return topicPublishInfo;
        }
    }

    private SendResult sendKernelImpl(Message msg, MessageQueue mq, CommunicationMode communicationMode, SendCallback sendCallback, TopicPublishInfo topicPublishInfo, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        long beginStartTime = System.currentTimeMillis();
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.tryToFindTopicPublishInfo(mq.getTopic());
            brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        SendMessageContext context = null;
        if (brokerAddr != null) {
            brokerAddr = MixAll.brokerVIPChannel(this.defaultMQProducer.isSendMessageWithVIPChannel(), brokerAddr);
            byte[] prevBody = msg.getBody();

            SendResult var38;
            try {
                if (!(msg instanceof MessageBatch)) {
                    MessageClientIDSetter.setUniqID(msg);
                    if (this.defaultMQProducer.isAddExtendUniqInfo()) {
                        MessageClientIDSetter.setExtendUniqInfo(msg, this.defaultMQProducer.getRandomSign());
                    }
                }

                boolean topicWithNamespace = false;
                if (null != this.mQClientFactory.getClientConfig().getNamespace()) {
                    msg.setInstanceId(this.mQClientFactory.getClientConfig().getNamespace());
                    topicWithNamespace = true;
                }

                int sysFlag = 0;
                boolean msgBodyCompressed = false;
                if (this.tryToCompressMessage(msg)) {
                    sysFlag |= 1;
                    msgBodyCompressed = true;
                }

                String tranMsg = msg.getProperty("TRAN_MSG");
                if (tranMsg != null && Boolean.parseBoolean(tranMsg)) {
                    sysFlag |= 4;
                }

                if (this.hasCheckForbiddenHook()) {
                    CheckForbiddenContext checkForbiddenContext = new CheckForbiddenContext();
                    checkForbiddenContext.setNameSrvAddr(this.defaultMQProducer.getNamesrvAddr());
                    checkForbiddenContext.setGroup(this.defaultMQProducer.getProducerGroup());
                    checkForbiddenContext.setCommunicationMode(communicationMode);
                    checkForbiddenContext.setBrokerAddr(brokerAddr);
                    checkForbiddenContext.setMessage(msg);
                    checkForbiddenContext.setMq(mq);
                    checkForbiddenContext.setUnitMode(this.isUnitMode());
                    this.executeCheckForbiddenHook(checkForbiddenContext);
                }

                if (this.hasSendMessageHook()) {
                    context = new SendMessageContext();
                    context.setProducer(this);
                    context.setProducerGroup(this.defaultMQProducer.getProducerGroup());
                    context.setCommunicationMode(communicationMode);
                    context.setBornHost(this.defaultMQProducer.getClientIP());
                    context.setBrokerAddr(brokerAddr);
                    context.setMessage(msg);
                    context.setMq(mq);
                    context.setNamespace(this.defaultMQProducer.getNamespace());
                    String isTrans = msg.getProperty("TRAN_MSG");
                    if (isTrans != null && isTrans.equals("true")) {
                        context.setMsgType(MessageType.Trans_Msg_Half);
                    }

                    if (msg.getProperty("__STARTDELIVERTIME") != null || msg.getProperty("DELAY") != null) {
                        context.setMsgType(MessageType.Delay_Msg);
                    }

                    this.executeSendMessageHookBefore(context);
                }

                SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
                requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
                requestHeader.setTopic(msg.getTopic());
                requestHeader.setDefaultTopic(this.defaultMQProducer.getCreateTopicKey());
                requestHeader.setDefaultTopicQueueNums(this.defaultMQProducer.getDefaultTopicQueueNums());
                requestHeader.setQueueId(mq.getQueueId());
                requestHeader.setSysFlag(sysFlag);
                requestHeader.setBornTimestamp(System.currentTimeMillis());
                requestHeader.setFlag(msg.getFlag());
                requestHeader.setProperties(MessageDecoder.messageProperties2String(msg.getProperties()));
                requestHeader.setReconsumeTimes(0);
                requestHeader.setUnitMode(this.isUnitMode());
                requestHeader.setBatch(msg instanceof MessageBatch);
                if (requestHeader.getTopic().startsWith("%RETRY%")) {
                    String reconsumeTimes = MessageAccessor.getReconsumeTime(msg);
                    if (reconsumeTimes != null) {
                        requestHeader.setReconsumeTimes(Integer.valueOf(reconsumeTimes));
                        MessageAccessor.clearProperty(msg, "RECONSUME_TIME");
                    }

                    String maxReconsumeTimes = MessageAccessor.getMaxReconsumeTimes(msg);
                    if (maxReconsumeTimes != null) {
                        requestHeader.setMaxReconsumeTimes(Integer.valueOf(maxReconsumeTimes));
                        MessageAccessor.clearProperty(msg, "MAX_RECONSUME_TIMES");
                    }
                }

                SendResult sendResult = null;
                switch(communicationMode) {
                    case ASYNC:
                        Message tmpMessage = msg;
                        boolean messageCloned = false;
                        if (msgBodyCompressed) {
                            tmpMessage = MessageAccessor.cloneMessage(msg);
                            msg.setBody(prevBody);
                        }

                        if (topicWithNamespace) {
                            if (!messageCloned) {
                                tmpMessage = MessageAccessor.cloneMessage(msg);
                                messageCloned = true;
                            }

                            msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQProducer.getNamespace()));
                        }

                        long costTimeAsync = System.currentTimeMillis() - beginStartTime;
                        if (timeout < costTimeAsync) {
                            throw new RemotingTooMuchRequestException("sendKernelImpl call timeout");
                        }

                        sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(brokerAddr, mq.getBrokerName(), tmpMessage, requestHeader, timeout - costTimeAsync, communicationMode, sendCallback, topicPublishInfo, this.mQClientFactory, this.defaultMQProducer.getRetryTimesWhenSendAsyncFailed(), context, this);
                        break;
                    case ONEWAY:
                    case SYNC:
                        long costTimeSync = System.currentTimeMillis() - beginStartTime;
                        if (timeout < costTimeSync) {
                            throw new RemotingTooMuchRequestException("sendKernelImpl call timeout");
                        }

                        sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(brokerAddr, mq.getBrokerName(), msg, requestHeader, timeout - costTimeSync, communicationMode, context, this);
                        break;
                    default:
                        assert false;
                }

                if (this.hasSendMessageHook()) {
                    context.setSendResult(sendResult);
                    this.executeSendMessageHookAfter(context);
                }

                var38 = sendResult;
            } catch (RemotingException var30) {
                if (this.hasSendMessageHook()) {
                    context.setException(var30);
                    this.executeSendMessageHookAfter(context);
                }

                throw var30;
            } catch (MQBrokerException var31) {
                if (this.hasSendMessageHook()) {
                    context.setException(var31);
                    this.executeSendMessageHookAfter(context);
                }

                throw var31;
            } catch (InterruptedException var32) {
                if (this.hasSendMessageHook()) {
                    context.setException(var32);
                    this.executeSendMessageHookAfter(context);
                }

                throw var32;
            } finally {
                msg.setBody(prevBody);
                msg.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), this.defaultMQProducer.getNamespace()));
            }

            return var38;
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable)null);
        }
    }

    public MQClientInstance getmQClientFactory() {
        return this.mQClientFactory;
    }

    private boolean tryToCompressMessage(Message msg) {
        if (msg instanceof MessageBatch) {
            return false;
        } else {
            byte[] body = msg.getBody();
            if (body != null && body.length >= this.defaultMQProducer.getCompressMsgBodyOverHowmuch()) {
                try {
                    byte[] data = UtilAll.compress(body, this.zipCompressLevel);
                    if (data != null) {
                        msg.setBody(data);
                        return true;
                    }
                } catch (IOException var4) {
                    this.log.error("tryToCompressMessage exception", var4);
                    this.log.warn(msg.toString());
                }
            }

            return false;
        }
    }

    public boolean hasCheckForbiddenHook() {
        return !this.checkForbiddenHookList.isEmpty();
    }

    public void executeCheckForbiddenHook(CheckForbiddenContext context) throws MQClientException {
        if (this.hasCheckForbiddenHook()) {
            Iterator var2 = this.checkForbiddenHookList.iterator();

            while(var2.hasNext()) {
                CheckForbiddenHook hook = (CheckForbiddenHook)var2.next();
                hook.checkForbidden(context);
            }
        }

    }

    public boolean hasSendMessageHook() {
        return !this.sendMessageHookList.isEmpty();
    }

    public void executeSendMessageHookBefore(SendMessageContext context) {
        if (!this.sendMessageHookList.isEmpty()) {
            Iterator var2 = this.sendMessageHookList.iterator();

            while(var2.hasNext()) {
                SendMessageHook hook = (SendMessageHook)var2.next();

                try {
                    hook.sendMessageBefore(context);
                } catch (Throwable var5) {
                    this.log.warn("failed to executeSendMessageHookBefore", var5);
                }
            }
        }

    }

    public void executeSendMessageHookAfter(SendMessageContext context) {
        if (!this.sendMessageHookList.isEmpty()) {
            Iterator var2 = this.sendMessageHookList.iterator();

            while(var2.hasNext()) {
                SendMessageHook hook = (SendMessageHook)var2.next();

                try {
                    hook.sendMessageAfter(context);
                } catch (Throwable var5) {
                    this.log.warn("failed to executeSendMessageHookAfter", var5);
                }
            }
        }

    }

    public void sendOneway(Message msg) throws MQClientException, RemotingException, InterruptedException {
        try {
            this.sendDefaultImpl(msg, CommunicationMode.ONEWAY, (SendCallback)null, (long)this.defaultMQProducer.getSendMsgTimeout());
        } catch (MQBrokerException var3) {
            throw new MQClientException("unknown exception", var3);
        }
    }

    public SendResult send(Message msg, MessageQueue mq) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.send(msg, mq, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    public SendResult send(Message msg, MessageQueue mq, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        long beginStartTime = System.currentTimeMillis();
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);
        if (!msg.getTopic().equals(mq.getTopic())) {
            throw new MQClientException("message's topic not equal mq's topic", (Throwable)null);
        } else {
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeout < costTime) {
                throw new RemotingTooMuchRequestException("call timeout");
            } else {
                return this.sendKernelImpl(msg, mq, CommunicationMode.SYNC, (SendCallback)null, (TopicPublishInfo)null, timeout);
            }
        }
    }

    public void send(Message msg, MessageQueue mq, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        this.send(msg, mq, sendCallback, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    /** @deprecated */
    @Deprecated
    public void send(final Message msg, final MessageQueue mq, final SendCallback sendCallback, final long timeout) throws MQClientException, RemotingException, InterruptedException {
        final long beginStartTime = System.currentTimeMillis();
        ExecutorService executor = this.getCallbackExecutor();

        try {
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        DefaultMQProducerImpl.this.makeSureStateOK();
                        Validators.checkMessage(msg, DefaultMQProducerImpl.this.defaultMQProducer);
                        if (!msg.getTopic().equals(mq.getTopic())) {
                            throw new MQClientException("message's topic not equal mq's topic", (Throwable)null);
                        }

                        long costTime = System.currentTimeMillis() - beginStartTime;
                        if (timeout > costTime) {
                            try {
                                DefaultMQProducerImpl.this.sendKernelImpl(msg, mq, CommunicationMode.ASYNC, sendCallback, (TopicPublishInfo)null, timeout - costTime);
                            } catch (MQBrokerException var4) {
                                throw new MQClientException("unknown exception", var4);
                            }
                        } else {
                            sendCallback.onException(new RemotingTooMuchRequestException("call timeout"));
                        }
                    } catch (Exception var5) {
                        sendCallback.onException(var5);
                    }

                }
            });
        } catch (RejectedExecutionException var10) {
            throw new MQClientException("executor rejected ", var10);
        }
    }

    public void sendOneway(Message msg, MessageQueue mq) throws MQClientException, RemotingException, InterruptedException {
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);

        try {
            this.sendKernelImpl(msg, mq, CommunicationMode.ONEWAY, (SendCallback)null, (TopicPublishInfo)null, (long)this.defaultMQProducer.getSendMsgTimeout());
        } catch (MQBrokerException var4) {
            throw new MQClientException("unknown exception", var4);
        }
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.send(msg, selector, arg, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.sendSelectImpl(msg, selector, arg, CommunicationMode.SYNC, (SendCallback)null, timeout);
    }

    private SendResult sendSelectImpl(Message msg, MessageQueueSelector selector, Object arg, CommunicationMode communicationMode, SendCallback sendCallback, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        long beginStartTime = System.currentTimeMillis();
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);
        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue mq = null;

            try {
                List<MessageQueue> messageQueueList = this.mQClientFactory.getMQAdminImpl().parsePublishMessageQueues(topicPublishInfo.getMessageQueueList());
                Message userMessage = MessageAccessor.cloneMessage(msg);
                String userTopic = NamespaceUtil.withoutNamespace(userMessage.getTopic(), this.mQClientFactory.getClientConfig().getNamespace());
                userMessage.setTopic(userTopic);
                mq = this.mQClientFactory.getClientConfig().queueWithNamespace(selector.select(messageQueueList, userMessage, arg));
            } catch (Throwable var15) {
                throw new MQClientException("select message queue throwed exception.", var15);
            }

            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeout < costTime) {
                throw new RemotingTooMuchRequestException("sendSelectImpl call timeout");
            } else if (mq != null) {
                return this.sendKernelImpl(msg, mq, communicationMode, sendCallback, (TopicPublishInfo)null, timeout - costTime);
            } else {
                throw new MQClientException("select message queue return null.", (Throwable)null);
            }
        } else {
            throw new MQClientException("No route info for this topic, " + msg.getTopic(), (Throwable)null);
        }
    }

    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        this.send(msg, selector, arg, sendCallback, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    /** @deprecated */
    @Deprecated
    public void send(final Message msg, final MessageQueueSelector selector, final Object arg, final SendCallback sendCallback, final long timeout) throws MQClientException, RemotingException, InterruptedException {
        final long beginStartTime = System.currentTimeMillis();
        ExecutorService executor = this.getCallbackExecutor();

        try {
            executor.submit(new Runnable() {
                public void run() {
                    long costTime = System.currentTimeMillis() - beginStartTime;
                    if (timeout > costTime) {
                        try {
                            try {
                                DefaultMQProducerImpl.this.sendSelectImpl(msg, selector, arg, CommunicationMode.ASYNC, sendCallback, timeout - costTime);
                            } catch (MQBrokerException var4) {
                                throw new MQClientException("unknownn exception", var4);
                            }
                        } catch (Exception var5) {
                            sendCallback.onException(var5);
                        }
                    } else {
                        sendCallback.onException(new RemotingTooMuchRequestException("call timeout"));
                    }

                }
            });
        } catch (RejectedExecutionException var11) {
            throw new MQClientException("exector rejected ", var11);
        }
    }

    public void sendOneway(Message msg, MessageQueueSelector selector, Object arg) throws MQClientException, RemotingException, InterruptedException {
        try {
            this.sendSelectImpl(msg, selector, arg, CommunicationMode.ONEWAY, (SendCallback)null, (long)this.defaultMQProducer.getSendMsgTimeout());
        } catch (MQBrokerException var5) {
            throw new MQClientException("unknown exception", var5);
        }
    }

    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecuter localTransactionExecuter, Object arg) throws MQClientException {
        TransactionListener transactionListener = this.getCheckListener();
        if (null == localTransactionExecuter && null == transactionListener) {
            throw new MQClientException("tranExecutor is null", (Throwable)null);
        } else {
            Validators.checkMessage(msg, this.defaultMQProducer);
            SendResult sendResult = null;
            MessageAccessor.putProperty(msg, "TRAN_MSG", "true");
            MessageAccessor.putProperty(msg, "PGROUP", this.defaultMQProducer.getProducerGroup());

            try {
                sendResult = this.send(msg);
            } catch (Exception var11) {
                throw new MQClientException("send message Exception", var11);
            }

            LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
            Throwable localException = null;
            switch(sendResult.getSendStatus()) {
                case SEND_OK:
                    try {
                        if (sendResult.getTransactionId() != null) {
                            msg.putUserProperty("__transactionId__", sendResult.getTransactionId());
                        }

                        String transactionId = msg.getProperty("UNIQ_KEY");
                        if (null != transactionId && !"".equals(transactionId)) {
                            msg.setTransactionId(transactionId);
                        }

                        if (null != localTransactionExecuter) {
                            localTransactionState = localTransactionExecuter.executeLocalTransactionBranch(msg, arg);
                        } else if (transactionListener != null) {
                            this.log.debug("Used new transaction API");
                            localTransactionState = transactionListener.executeLocalTransaction(msg, arg);
                        }

                        if (null == localTransactionState) {
                            localTransactionState = LocalTransactionState.UNKNOW;
                        }

                        if (localTransactionState != LocalTransactionState.COMMIT_MESSAGE) {
                            this.log.info("executeLocalTransactionBranch return {}", localTransactionState);
                            this.log.info(msg.toString());
                        }
                    } catch (Throwable var10) {
                        this.log.info("executeLocalTransactionBranch exception", var10);
                        this.log.info(msg.toString());
                        localException = var10;
                    }
                    break;
                case FLUSH_DISK_TIMEOUT:
                case FLUSH_SLAVE_TIMEOUT:
                case SLAVE_NOT_AVAILABLE:
                    localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
            }

            try {
                this.endTransaction(sendResult, localTransactionState, localException);
            } catch (Exception var9) {
                this.log.warn("local transaction execute " + localTransactionState + ", but end broker transaction failed", var9);
            }

            TransactionSendResult transactionSendResult = new TransactionSendResult();
            transactionSendResult.setSendStatus(sendResult.getSendStatus());
            transactionSendResult.setMessageQueue(sendResult.getMessageQueue());
            transactionSendResult.setMsgId(sendResult.getMsgId());
            transactionSendResult.setQueueOffset(sendResult.getQueueOffset());
            transactionSendResult.setTransactionId(sendResult.getTransactionId());
            transactionSendResult.setLocalTransactionState(localTransactionState);
            return transactionSendResult;
        }
    }

    public SendResult send(Message msg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.send(msg, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    public void endTransaction(SendResult sendResult, LocalTransactionState localTransactionState, Throwable localException) throws RemotingException, MQBrokerException, InterruptedException, UnknownHostException {
        MessageId id;
        if (sendResult.getOffsetMsgId() != null) {
            id = MessageDecoder.decodeMessageId(sendResult.getOffsetMsgId());
        } else {
            id = MessageDecoder.decodeMessageId(sendResult.getMsgId());
        }

        String transactionId = sendResult.getTransactionId();
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(sendResult.getMessageQueue().getBrokerName());
        EndTransactionRequestHeader requestHeader = new EndTransactionRequestHeader();
        requestHeader.setTransactionId(transactionId);
        requestHeader.setCommitLogOffset(id.getOffset());
        switch(localTransactionState) {
            case COMMIT_MESSAGE:
                requestHeader.setCommitOrRollback(8);
                break;
            case ROLLBACK_MESSAGE:
                requestHeader.setCommitOrRollback(12);
                break;
            case UNKNOW:
                requestHeader.setCommitOrRollback(0);
        }

        requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
        requestHeader.setTranStateTableOffset(sendResult.getQueueOffset());
        requestHeader.setMsgId(sendResult.getMsgId());
        String remark = localException != null ? "executeLocalTransactionBranch exception: " + localException.toString() : null;
        this.mQClientFactory.getMQClientAPIImpl().endTransactionOneway(brokerAddr, requestHeader, remark, (long)this.defaultMQProducer.getSendMsgTimeout());
    }

    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        this.mQClientFactory.getMQClientAPIImpl().getRemotingClient().setCallbackExecutor(callbackExecutor);
    }

    public ExecutorService getCallbackExecutor() {
        return this.mQClientFactory.getMQClientAPIImpl().getRemotingClient().getCallbackExecutor();
    }

    public SendResult send(Message msg, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.sendDefaultImpl(msg, CommunicationMode.SYNC, (SendCallback)null, timeout);
    }

    public ConcurrentMap<String, TopicPublishInfo> getTopicPublishInfoTable() {
        return this.topicPublishInfoTable;
    }

    public int getZipCompressLevel() {
        return this.zipCompressLevel;
    }

    public void setZipCompressLevel(int zipCompressLevel) {
        this.zipCompressLevel = zipCompressLevel;
    }

    public ServiceState getServiceState() {
        return this.serviceState;
    }

    public void setServiceState(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public long[] getNotAvailableDuration() {
        return this.mqFaultStrategy.getNotAvailableDuration();
    }

    public void setNotAvailableDuration(long[] notAvailableDuration) {
        this.mqFaultStrategy.setNotAvailableDuration(notAvailableDuration);
    }

    public long[] getLatencyMax() {
        return this.mqFaultStrategy.getLatencyMax();
    }

    public void setLatencyMax(long[] latencyMax) {
        this.mqFaultStrategy.setLatencyMax(latencyMax);
    }

    public boolean isSendLatencyFaultEnable() {
        return this.mqFaultStrategy.isSendLatencyFaultEnable();
    }

    public void setSendLatencyFaultEnable(boolean sendLatencyFaultEnable) {
        this.mqFaultStrategy.setSendLatencyFaultEnable(sendLatencyFaultEnable);
    }
}
