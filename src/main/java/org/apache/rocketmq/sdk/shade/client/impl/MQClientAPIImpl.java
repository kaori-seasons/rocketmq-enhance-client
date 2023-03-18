package org.apache.rocketmq.sdk.shade.client.impl;

import org.apache.rocketmq.sdk.shade.client.ClientConfig;
import org.apache.rocketmq.sdk.shade.client.consumer.PullCallback;
import org.apache.rocketmq.sdk.shade.client.consumer.PullResult;
import org.apache.rocketmq.sdk.shade.client.consumer.PullStatus;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.hook.SendMessageContext;
import org.apache.rocketmq.sdk.shade.client.impl.consumer.PullResultExt;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.sdk.shade.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.producer.SendCallback;
import org.apache.rocketmq.sdk.shade.client.producer.SendResult;
import org.apache.rocketmq.sdk.shade.client.producer.SendStatus;
import org.apache.rocketmq.sdk.shade.common.MQVersion;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.TopicConfig;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.admin.ConsumeStats;
import org.apache.rocketmq.sdk.shade.common.admin.TopicStatsTable;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageBatch;
import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.namesrv.TopAddressing;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.RequestCode;
import org.apache.rocketmq.sdk.shade.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.sdk.shade.common.protocol.body.CheckClientRequestBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeStatsList;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.body.GetConsumerStatusBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.GroupList;
import org.apache.rocketmq.sdk.shade.common.protocol.body.KVTable;
import org.apache.rocketmq.sdk.shade.common.protocol.body.LockBatchRequestBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.LockBatchResponseBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.sdk.shade.common.protocol.body.QueryConsumeQueueResponseBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.QueryConsumeTimeSpanBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.QueryCorrectionOffsetBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.QueueTimeSpan;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ResetOffsetBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.sdk.shade.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.sdk.shade.common.protocol.body.TopicList;
import org.apache.rocketmq.sdk.shade.common.protocol.body.UnlockBatchRequestBody;
import org.apache.rocketmq.sdk.shade.common.protocol.header.CloneGroupOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ConsumeMessageDirectlyResultRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ConsumerSendMsgBackRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.EndTransactionRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumeStatsInBrokerHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerListByGroupRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerListByGroupResponseBody;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerRunningInfoRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerStatusRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetEarliestMsgStoretimeRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetEarliestMsgStoretimeResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetMaxOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetMaxOffsetResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetMinOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetMinOffsetResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetProducerConnectionListRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetTopicStatsInfoRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetTopicsByClusterRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.PullMessageResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryConsumeQueueRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryConsumeTimeSpanRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryConsumerOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryConsumerOffsetResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryCorrectionOffsetHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryTopicConsumeByWhoRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ResetOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SearchOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SearchOffsetResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SendMessageRequestHeaderV2;
import org.apache.rocketmq.sdk.shade.common.protocol.header.SendMessageResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.UnregisterClientRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ViewBrokerStatsDataRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ViewMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.filtersrv.RegisterMessageFilterClassRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.iam.RegisterOrUpdateTopicUserRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.DeleteKVConfigRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.GetKVConfigRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.GetKVConfigResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.GetKVListByNamespaceRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.PutKVConfigRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.WipeWritePermOfBrokerRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.WipeWritePermOfBrokerResponseHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.sdk.shade.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.RemotingClient;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyRemotingProxyClient;
import org.apache.rocketmq.sdk.shade.remoting.netty.ResponseFuture;
import org.apache.rocketmq.sdk.shade.remoting.protocol.LanguageCode;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class MQClientAPIImpl {
    private static final InternalLogger log;
    private static boolean sendSmartMsg;
    private final RemotingClient remotingClient;
    private final TopAddressing topAddressing;
    private final ClientRemotingProcessor clientRemotingProcessor;
    private String nameSrvAddr = null;
    private String proxyAddr = null;
    private ClientConfig clientConfig;
    static final boolean assertResult;

    static {
        assertResult = !MQClientAPIImpl.class.desiredAssertionStatus();
        log = ClientLogger.getLog();
        sendSmartMsg = Boolean.parseBoolean(System.getProperty("org.apache.rocketmq.client.sendSmartMsg", "true"));
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
    }

    public MQClientAPIImpl(NettyClientConfig nettyClientConfig, ClientRemotingProcessor clientRemotingProcessor, RPCHook rpcHook, ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.topAddressing = new TopAddressing(MixAll.getWSAddr(), clientConfig.getUnitName());
        if (this.clientConfig.getNamesrvAddr() != null) {
            this.remotingClient = new NettyRemotingClient(nettyClientConfig, null);
        } else {
            this.remotingClient = new NettyRemotingProxyClient(nettyClientConfig, null);
        }
        this.clientRemotingProcessor = clientRemotingProcessor;
        this.remotingClient.registerRPCHook(rpcHook);
        this.remotingClient.registerProcessor(39, this.clientRemotingProcessor, null);
        this.remotingClient.registerProcessor(40, this.clientRemotingProcessor, null);
        this.remotingClient.registerProcessor(RequestCode.RESET_CONSUMER_CLIENT_OFFSET, this.clientRemotingProcessor, null);
        this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT, this.clientRemotingProcessor, null);
        this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_RUNNING_INFO, this.clientRemotingProcessor, null);
        this.remotingClient.registerProcessor(RequestCode.CONSUME_MESSAGE_DIRECTLY, this.clientRemotingProcessor, null);
    }

    public List<String> getNameServerAddressList() {
        return this.remotingClient.getNameServerAddressList();
    }

    public RemotingClient getRemotingClient() {
        return this.remotingClient;
    }

    public String fetchNameServerAddr() {
        try {
            String addrs = this.topAddressing.fetchNSAddr();
            if (addrs != null && !addrs.equals(this.nameSrvAddr)) {
                log.info("name server address changed, old=" + this.nameSrvAddr + ", new=" + addrs);
                updateNameServerAddressList(addrs);
                this.nameSrvAddr = addrs;
                return this.nameSrvAddr;
            }
        } catch (Exception e) {
            log.error("fetchNameServerAddr Exception", (Throwable) e);
        }
        return this.nameSrvAddr;
    }

    public void updateNameServerAddressList(String addrs) {
        this.remotingClient.updateNameServerAddressList(Arrays.asList(addrs.split(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR)));
    }

    public void updateProxyAddressList(String addrs) {
        this.remotingClient.updateProxyAddressList(Arrays.asList(addrs.split(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR)));
    }

    public String fetchProxyAddr() {
        try {
            String addrs = this.topAddressing.fetchNSAddr();
            if (addrs != null && !addrs.equals(this.proxyAddr)) {
                log.info("proxy address changed, old=" + this.proxyAddr + ", new=" + addrs);
                updateProxyAddressList(addrs);
                this.proxyAddr = addrs;
                return this.proxyAddr;
            }
        } catch (Exception e) {
            log.error("fetchProxyAddr Exception", (Throwable) e);
        }
        return this.proxyAddr;
    }

    public void start() {
        this.remotingClient.start();
    }

    public void shutdown() {
        this.remotingClient.shutdown();
    }

    public void createSubscriptionGroup(String addr, SubscriptionGroupConfig config, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        RemotingCommand request = RemotingCommand.createRequestCommand(200, null);
        request.setBody(RemotingSerializable.encode(config));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void createTopic(String addr, String defaultTopic, TopicConfig topicConfig, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
        requestHeader.setTopic(topicConfig.getTopicName());
        requestHeader.setDefaultTopic(defaultTopic);
        requestHeader.setReadQueueNums(Integer.valueOf(topicConfig.getReadQueueNums()));
        requestHeader.setWriteQueueNums(Integer.valueOf(topicConfig.getWriteQueueNums()));
        requestHeader.setPerm(Integer.valueOf(topicConfig.getPerm()));
        requestHeader.setTopicFilterType(topicConfig.getTopicFilterType().name());
        requestHeader.setTopicSysFlag(Integer.valueOf(topicConfig.getTopicSysFlag()));
        requestHeader.setOrder(Boolean.valueOf(topicConfig.isOrder()));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(17, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void createTopic(String iamAddr, String topicName, String userId, int perm, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        RegisterOrUpdateTopicUserRequestHeader requestHeader = new RegisterOrUpdateTopicUserRequestHeader();
        requestHeader.setTopic(topicName);
        requestHeader.setUserId(userId);
        requestHeader.setPerm(perm);
        RemotingCommand response = this.remotingClient.invokeSync(iamAddr, RemotingCommand.createRequestCommand(RequestCode.REGISTER_TOPIC_USER, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public SendResult sendMessage(String addr, String brokerName, Message msg, SendMessageRequestHeader requestHeader, long timeoutMillis, CommunicationMode communicationMode, SendMessageContext context, DefaultMQProducerImpl producer) throws RemotingException, MQBrokerException, InterruptedException {
        return sendMessage(addr, brokerName, msg, requestHeader, timeoutMillis, communicationMode, null, null, null, 0, context, producer);
    }

    public SendResult sendMessage(String addr, String brokerName, Message msg, SendMessageRequestHeader requestHeader, long timeoutMillis, CommunicationMode communicationMode, SendCallback sendCallback, TopicPublishInfo topicPublishInfo, MQClientInstance instance, int retryTimesWhenSendFailed, SendMessageContext context, DefaultMQProducerImpl producer) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request;
        long beginStartTime = System.currentTimeMillis();
        if (sendSmartMsg || (msg instanceof MessageBatch)) {
            request = RemotingCommand.createRequestCommand(msg instanceof MessageBatch ? RequestCode.SEND_BATCH_MESSAGE : RequestCode.SEND_MESSAGE_V2, SendMessageRequestHeaderV2.createSendMessageRequestHeaderV2(requestHeader));
        } else {
            request = RemotingCommand.createRequestCommand(10, requestHeader);
        }
        request.setBody(msg.getBody());
        switch (communicationMode) {
            case ONEWAY:
                this.remotingClient.invokeOneway(addr, request, timeoutMillis);
                return null;
            case ASYNC:
                AtomicInteger times = new AtomicInteger();
                long costTimeAsync = System.currentTimeMillis() - beginStartTime;
                if (timeoutMillis < costTimeAsync) {
                    throw new RemotingTooMuchRequestException("sendMessage call timeout");
                }
                sendMessageAsync(addr, brokerName, msg, timeoutMillis - costTimeAsync, request, sendCallback, topicPublishInfo, instance, retryTimesWhenSendFailed, times, context, producer);
                return null;
            case SYNC:
                long costTimeSync = System.currentTimeMillis() - beginStartTime;
                if (timeoutMillis >= costTimeSync) {
                    return sendMessageSync(addr, brokerName, msg, timeoutMillis - costTimeSync, request);
                }
                throw new RemotingTooMuchRequestException("sendMessage call timeout");
            default:
                if (assertResult) {
                    return null;
                }
                throw new AssertionError();
        }
    }

    private SendResult sendMessageSync(String addr, String brokerName, Message msg, long timeoutMillis, RemotingCommand request) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        if (assertResult || response != null) {
            return processSendResponse(brokerName, msg, response);
        }
        throw new AssertionError();
    }

    private void sendMessageAsync(String addr, final String brokerName, final Message msg, long timeoutMillis, final RemotingCommand request, final SendCallback sendCallback, final TopicPublishInfo topicPublishInfo, final MQClientInstance instance, final int retryTimesWhenSendFailed, final AtomicInteger times, final SendMessageContext context, final DefaultMQProducerImpl producer) throws InterruptedException, RemotingException {
        this.remotingClient.invokeAsync(addr, request, timeoutMillis, new InvokeCallback() {
            @Override
            public void operationComplete(ResponseFuture responseFuture) {
                RemotingCommand response = responseFuture.getResponseCommand();
                if (null == sendCallback && response != null) {

                    try {
                        SendResult sendResult = MQClientAPIImpl.this.processSendResponse(brokerName, msg, response);
                        if (context != null && sendResult != null) {
                            context.setSendResult(sendResult);
                            context.getProducer().executeSendMessageHookAfter(context);
                        }
                    } catch (Throwable throwable) {}


                    producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), false);

                    return;
                }
                if (response != null) {
                    try {
                        SendResult sendResult = MQClientAPIImpl.this.processSendResponse(brokerName, msg, response);
                        assert sendResult != null;
                        if (context != null) {
                            context.setSendResult(sendResult);
                            context.getProducer().executeSendMessageHookAfter(context);
                        }

                        try {
                            sendCallback.onSuccess(sendResult);
                        } catch (Throwable throwable) {}


                        producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), false);
                    } catch (Exception e) {
                        producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), true);
                        MQClientAPIImpl.this.onExceptionImpl(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance, retryTimesWhenSendFailed, times, e, context, false, producer);
                    }
                } else {

                    producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), true);
                    if (!responseFuture.isSendRequestOK()) {
                        MQClientException ex = new MQClientException("send request failed", responseFuture.getCause());
                        MQClientAPIImpl.this.onExceptionImpl(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance, retryTimesWhenSendFailed, times, (Exception)ex, context, true, producer);
                    }
                    else if (responseFuture.isTimeout()) {

                        MQClientException ex = new MQClientException("wait response timeout " + responseFuture.getTimeoutMillis() + "ms", responseFuture.getCause());
                        MQClientAPIImpl.this.onExceptionImpl(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance, retryTimesWhenSendFailed, times, (Exception)ex, context, true, producer);
                    } else {

                        MQClientException ex = new MQClientException("unknow reseaon", responseFuture.getCause());
                        MQClientAPIImpl.this.onExceptionImpl(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance, retryTimesWhenSendFailed, times, (Exception)ex, context, true, producer);
                    }
                }
            }
        });
    }

    public void onExceptionImpl(String brokerName, Message msg, long timeoutMillis, RemotingCommand request, SendCallback sendCallback, TopicPublishInfo topicPublishInfo, MQClientInstance instance, int timesTotal, AtomicInteger curTimes, Exception e, SendMessageContext context, boolean needRetry, DefaultMQProducerImpl producer) {
        int tmp = curTimes.incrementAndGet();
        if (!needRetry || tmp > timesTotal) {
            if (context != null) {
                context.setException(e);
                context.getProducer().executeSendMessageHookAfter(context);
            }
            try {
                sendCallback.onException(e);
            } catch (Exception e2) {
            }
        } else {
            String retryBrokerName = brokerName;
            if (topicPublishInfo != null) {
                retryBrokerName = producer.selectOneMessageQueue(topicPublishInfo, brokerName).getBrokerName();
            }
            String addr = instance.findBrokerAddressInPublish(retryBrokerName);
            log.info("async send msg by retry {} times. topic={}, brokerAddr={}, brokerName={}", Integer.valueOf(tmp), msg.getTopic(), addr, retryBrokerName);
            try {
                request.setOpaque(RemotingCommand.createNewRequestId());
                sendMessageAsync(addr, retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, context, producer);
            } catch (RemotingConnectException e1) {
                producer.updateFaultItem(brokerName, 3000, true);
                onExceptionImpl(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e1, context, true, producer);
            } catch (RemotingTooMuchRequestException e12) {
                onExceptionImpl(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e12, context, false, producer);
            } catch (RemotingException e13) {
                producer.updateFaultItem(brokerName, 3000, true);
                onExceptionImpl(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e13, context, true, producer);
            } catch (InterruptedException e14) {
                onExceptionImpl(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e14, context, false, producer);
            }
        }
    }

    public SendResult processSendResponse(String brokerName, Message msg, RemotingCommand response) throws MQBrokerException, RemotingCommandException {
        switch (response.getCode()) {
            case 0:
            case 10:
            case 11:
            case 12:
                SendStatus sendStatus = SendStatus.SEND_OK;
                switch (response.getCode()) {
                    case 0:
                        sendStatus = SendStatus.SEND_OK;
                        break;
                    case 10:
                        sendStatus = SendStatus.FLUSH_DISK_TIMEOUT;
                        break;
                    case 11:
                        sendStatus = SendStatus.SLAVE_NOT_AVAILABLE;
                        break;
                    case 12:
                        sendStatus = SendStatus.FLUSH_SLAVE_TIMEOUT;
                        break;
                    default:
                        if (!assertResult) {
                            throw new AssertionError();
                        }
                        break;
                }
                SendMessageResponseHeader responseHeader = (SendMessageResponseHeader) response.decodeCommandCustomHeader(SendMessageResponseHeader.class);
                String topic = msg.getTopic();
                if (StringUtils.isNotEmpty(this.clientConfig.getNamespace())) {
                    topic = NamespaceUtil.withoutNamespace(topic, this.clientConfig.getNamespace());
                }
                MessageQueue messageQueue = new MessageQueue(topic, brokerName, responseHeader.getQueueId().intValue());
                String uniqMsgId = MessageClientIDSetter.getUniqID(msg);
                if (msg instanceof MessageBatch) {
                    StringBuilder sb = new StringBuilder();
                    Iterator<Message> it = ((MessageBatch) msg).iterator();
                    while (it.hasNext()) {
                        sb.append(sb.length() == 0 ? "" : StringArrayPropertyEditor.DEFAULT_SEPARATOR).append(MessageClientIDSetter.getUniqID(it.next()));
                    }
                    uniqMsgId = sb.toString();
                }
                SendResult sendResult = new SendResult(sendStatus, uniqMsgId, responseHeader.getMsgId(), messageQueue, responseHeader.getQueueOffset().longValue());
                sendResult.setTransactionId(responseHeader.getTransactionId());
                String regionId = response.getExtFields().get(MessageConst.PROPERTY_MSG_REGION);
                String traceOn = response.getExtFields().get(MessageConst.PROPERTY_TRACE_SWITCH);
                if (regionId == null || regionId.isEmpty()) {
                    regionId = "DefaultRegion";
                }
                if (traceOn == null || !traceOn.equals("false")) {
                    sendResult.setTraceOn(true);
                } else {
                    sendResult.setTraceOn(false);
                }
                sendResult.setRegionId(regionId);
                return sendResult;
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public PullResult pullMessage(String addr, PullMessageRequestHeader requestHeader, long timeoutMillis, CommunicationMode communicationMode, PullCallback pullCallback) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(11, requestHeader);
        switch (communicationMode) {
            case ONEWAY:
                if (assertResult) {
                    return null;
                }
                throw new AssertionError();
            case ASYNC:
                pullMessageAsync(addr, request, timeoutMillis, pullCallback);
                return null;
            case SYNC:
                return pullMessageSync(addr, request, timeoutMillis);
            default:
                if (assertResult) {
                    return null;
                }
                throw new AssertionError();
        }
    }

    private void pullMessageAsync(final String addr, final RemotingCommand request, final long timeoutMillis, final PullCallback pullCallback) throws RemotingException, InterruptedException {
        this.remotingClient.invokeAsync(addr, request, timeoutMillis, new InvokeCallback() {
            @Override
            public void operationComplete(ResponseFuture responseFuture) {
                RemotingCommand response = responseFuture.getResponseCommand();
                if (response != null) {
                    try {
                        PullResult pullResult = MQClientAPIImpl.this.processPullResponse(response);
                        if (assertResult || pullResult != null) {
                            pullCallback.onSuccess(pullResult);
                            return;
                        }
                        throw new AssertionError();
                    } catch (Exception e) {
                        pullCallback.onException(e);
                    }
                } else if (!responseFuture.isSendRequestOK()) {
                    pullCallback.onException(new MQClientException("send request failed to " + addr + ". Request: " + request, responseFuture.getCause()));
                } else if (responseFuture.isTimeout()) {
                    pullCallback.onException(new MQClientException("wait response from " + addr + " timeout :" + responseFuture.getTimeoutMillis() + "ms. Request: " + request, responseFuture.getCause()));
                } else {
                    pullCallback.onException(new MQClientException("unknown reason. addr: " + addr + ", timeoutMillis: " + timeoutMillis + ". Request: " + request, responseFuture.getCause()));
                }
            }
        });
    }

    private PullResult pullMessageSync(String addr, RemotingCommand request, long timeoutMillis) throws RemotingException, InterruptedException, MQBrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        if (assertResult || response != null) {
            return processPullResponse(response);
        }
        throw new AssertionError();
    }

    public PullResult processPullResponse(RemotingCommand response) throws MQBrokerException, RemotingCommandException {
        PullStatus pullStatus;
        PullStatus pullStatus2 = PullStatus.NO_NEW_MSG;
        switch (response.getCode()) {
            case 0:
                pullStatus = PullStatus.FOUND;
                break;
            case 19:
                pullStatus = PullStatus.NO_NEW_MSG;
                break;
            case 20:
                pullStatus = PullStatus.NO_MATCHED_MSG;
                break;
            case 21:
                pullStatus = PullStatus.OFFSET_ILLEGAL;
                break;
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
        PullMessageResponseHeader responseHeader = (PullMessageResponseHeader) response.decodeCommandCustomHeader(PullMessageResponseHeader.class);
        return new PullResultExt(pullStatus, responseHeader.getNextBeginOffset().longValue(), responseHeader.getMinOffset().longValue(), responseHeader.getMaxOffset().longValue(), null, responseHeader.getSuggestWhichBrokerId().longValue(), response.getBody());
    }

    public MessageExt viewMessage(String addr, long phyoffset, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        ViewMessageRequestHeader requestHeader = new ViewMessageRequestHeader();
        requestHeader.setOffset(Long.valueOf(phyoffset));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(33, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    MessageExt messageExt = MessageDecoder.clientDecode(ByteBuffer.wrap(response.getBody()), true);
                    if (StringUtils.isNotEmpty(this.clientConfig.getNamespace())) {
                        messageExt.setTopic(NamespaceUtil.withoutNamespace(messageExt.getTopic(), this.clientConfig.getNamespace()));
                    }
                    return messageExt;
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public long searchOffset(String addr, String topic, int queueId, long timestamp, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        SearchOffsetRequestHeader requestHeader = new SearchOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(Integer.valueOf(queueId));
        requestHeader.setTimestamp(Long.valueOf(timestamp));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(29, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((SearchOffsetResponseHeader) response.decodeCommandCustomHeader(SearchOffsetResponseHeader.class)).getOffset().longValue();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public long getMaxOffset(String addr, String topic, int queueId, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        GetMaxOffsetRequestHeader requestHeader = new GetMaxOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(Integer.valueOf(queueId));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(30, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((GetMaxOffsetResponseHeader) response.decodeCommandCustomHeader(GetMaxOffsetResponseHeader.class)).getOffset().longValue();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public List<String> getConsumerIdListByGroup(String addr, String consumerGroup, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQBrokerException, InterruptedException {
        GetConsumerListByGroupRequestHeader requestHeader = new GetConsumerListByGroupRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(38, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        return ((GetConsumerListByGroupResponseBody) GetConsumerListByGroupResponseBody.decode(response.getBody(), GetConsumerListByGroupResponseBody.class)).getConsumerIdList();
                    }
                    break;
            }
            throw new MQBrokerException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public long getMinOffset(String addr, String topic, int queueId, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        GetMinOffsetRequestHeader requestHeader = new GetMinOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(Integer.valueOf(queueId));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(31, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((GetMinOffsetResponseHeader) response.decodeCommandCustomHeader(GetMinOffsetResponseHeader.class)).getOffset().longValue();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public long getEarliestMsgStoretime(String addr, String topic, int queueId, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        GetEarliestMsgStoretimeRequestHeader requestHeader = new GetEarliestMsgStoretimeRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(Integer.valueOf(queueId));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(32, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((GetEarliestMsgStoretimeResponseHeader) response.decodeCommandCustomHeader(GetEarliestMsgStoretimeResponseHeader.class)).getTimestamp().longValue();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public long queryConsumerOffset(String addr, QueryConsumerOffsetRequestHeader requestHeader, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(14, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((QueryConsumerOffsetResponseHeader) response.decodeCommandCustomHeader(QueryConsumerOffsetResponseHeader.class)).getOffset().longValue();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void updateConsumerOffset(String addr, UpdateConsumerOffsetRequestHeader requestHeader, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(15, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void updateConsumerOffsetOneway(String addr, UpdateConsumerOffsetRequestHeader requestHeader, long timeoutMillis) throws RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException, InterruptedException {
        this.remotingClient.invokeOneway(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(15, requestHeader), timeoutMillis);
    }

    public int sendHearbeat(String addr, HeartbeatData heartbeatData, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(34, null);
        request.setLanguage(this.clientConfig.getLanguage());
        request.setBody(heartbeatData.encode());
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return response.getVersion();
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void unregisterClient(String addr, String clientID, String producerGroup, String consumerGroup, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        UnregisterClientRequestHeader requestHeader = new UnregisterClientRequestHeader();
        requestHeader.setClientID(clientID);
        requestHeader.setProducerGroup(producerGroup);
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand response = this.remotingClient.invokeSync(addr, RemotingCommand.createRequestCommand(35, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void endTransactionOneway(String addr, EndTransactionRequestHeader requestHeader, String remark, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(37, requestHeader);
        request.setRemark(remark);
        this.remotingClient.invokeOneway(addr, request, timeoutMillis);
    }

    public void queryMessage(String addr, QueryMessageRequestHeader requestHeader, long timeoutMillis, InvokeCallback invokeCallback, Boolean isUnqiueKey) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(12, requestHeader);
        request.addExtField(MixAll.UNIQUE_MSG_QUERY_FLAG, isUnqiueKey.toString());
        this.remotingClient.invokeAsync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis, invokeCallback);
    }

    public boolean registerClient(String addr, HeartbeatData heartbeat, long timeoutMillis) throws RemotingException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(34, null);
        request.setBody(heartbeat.encode());
        return this.remotingClient.invokeSync(addr, request, timeoutMillis).getCode() == 0;
    }

    public void consumerSendMessageBack(String addr, MessageExt msg, String consumerGroup, int delayLevel, long timeoutMillis, int maxConsumeRetryTimes) throws RemotingException, MQBrokerException, InterruptedException {
        ConsumerSendMsgBackRequestHeader requestHeader = new ConsumerSendMsgBackRequestHeader();
        RemotingCommand request = RemotingCommand.createRequestCommand(36, requestHeader);
        requestHeader.setGroup(consumerGroup);
        requestHeader.setOriginTopic(msg.getTopic());
        requestHeader.setOffset(Long.valueOf(msg.getCommitLogOffset()));
        requestHeader.setDelayLevel(Integer.valueOf(delayLevel));
        requestHeader.setOriginMsgId(msg.getMsgId());
        requestHeader.setMaxReconsumeTimes(Integer.valueOf(maxConsumeRetryTimes));
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public Set<MessageQueue> lockBatchMQ(String addr, LockBatchRequestBody requestBody, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(41, null);
        request.setBody(requestBody.encode());
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return ((LockBatchResponseBody) LockBatchResponseBody.decode(response.getBody(), LockBatchResponseBody.class)).getLockOKMQSet();
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public void unlockBatchMQ(String addr, UnlockBatchRequestBody requestBody, long timeoutMillis, boolean oneway) throws RemotingException, MQBrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(42, null);
        request.setBody(requestBody.encode());
        if (oneway) {
            this.remotingClient.invokeOneway(addr, request, timeoutMillis);
            return;
        }
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return;
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public TopicStatsTable getTopicStatsInfo(String addr, String topic, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        GetTopicStatsInfoRequestHeader requestHeader = new GetTopicStatsInfoRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(202, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (TopicStatsTable) TopicStatsTable.decode(response.getBody(), TopicStatsTable.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public ConsumeStats getConsumeStats(String addr, String consumerGroup, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return getConsumeStats(addr, consumerGroup, null, timeoutMillis);
    }

    public ConsumeStats getConsumeStats(String addr, String consumerGroup, String topic, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        GetConsumeStatsRequestHeader requestHeader = new GetConsumeStatsRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(208, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (ConsumeStats) ConsumeStats.decode(response.getBody(), ConsumeStats.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public ProducerConnection getProducerConnectionList(String addr, String producerGroup, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        GetProducerConnectionListRequestHeader requestHeader = new GetProducerConnectionListRequestHeader();
        requestHeader.setProducerGroup(producerGroup);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(204, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (ProducerConnection) ProducerConnection.decode(response.getBody(), ProducerConnection.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public ConsumerConnection getConsumerConnectionList(String addr, String consumerGroup, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        GetConsumerConnectionListRequestHeader requestHeader = new GetConsumerConnectionListRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(203, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (ConsumerConnection) ConsumerConnection.decode(response.getBody(), ConsumerConnection.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public KVTable getBrokerRuntimeInfo(String addr, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(28, null), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (KVTable) KVTable.decode(response.getBody(), KVTable.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public void updateBrokerConfig(String addr, Properties properties, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException, UnsupportedEncodingException {
        RemotingCommand request = RemotingCommand.createRequestCommand(25, null);
        String str = MixAll.properties2String(properties);
        if (str != null && str.length() > 0) {
            request.setBody(str.getBytes("UTF-8"));
            RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        }
    }

    public Properties getBrokerConfig(String addr, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException, UnsupportedEncodingException {
        RemotingCommand response = this.remotingClient.invokeSync(addr, RemotingCommand.createRequestCommand(26, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return MixAll.string2Properties(new String(response.getBody(), "UTF-8"));
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public ClusterInfo getBrokerClusterInfo(long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(106, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return (ClusterInfo) ClusterInfo.decode(response.getBody(), ClusterInfo.class);
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public TopicRouteData getDefaultTopicRouteInfoFromNameServer(String topic, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        return getTopicRouteInfoFromNameServer(topic, timeoutMillis, false);
    }

    public TopicRouteData getTopicRouteInfoFromNameServer(String topic, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        return getTopicRouteInfoFromNameServer(topic, timeoutMillis, true);
    }

    public TopicRouteData getTopicRouteInfoFromNameServer(String topic, long timeoutMillis, boolean allowTopicNotExist) throws MQClientException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        GetRouteInfoRequestHeader requestHeader = new GetRouteInfoRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(105, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (TopicRouteData) TopicRouteData.decode(body, TopicRouteData.class);
                    }
                    break;
                case 17:
                    if (allowTopicNotExist && !topic.equals(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC)) {
                        log.warn("get Topic [{}] RouteInfoFromNameServer is not exist value", topic);
                        break;
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public TopicList getTopicListFromNameServer(long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(206, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (TopicList) TopicList.decode(body, TopicList.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName, long timeoutMillis) throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQClientException {
        WipeWritePermOfBrokerRequestHeader requestHeader = new WipeWritePermOfBrokerRequestHeader();
        requestHeader.setBrokerName(brokerName);
        RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, RemotingCommand.createRequestCommand(205, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((WipeWritePermOfBrokerResponseHeader) response.decodeCommandCustomHeader(WipeWritePermOfBrokerResponseHeader.class)).getWipeTopicCount().intValue();
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void deleteTopicInBroker(String addr, String topic, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void deleteTopicInNameServer(String addr, String topic, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(addr, RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_NAMESRV, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void deleteSubscriptionGroup(String addr, String groupName, long timeoutMillis) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        DeleteSubscriptionGroupRequestHeader requestHeader = new DeleteSubscriptionGroupRequestHeader();
        requestHeader.setGroupName(groupName);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(207, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public String getKVConfigValue(String namespace, String key, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        GetKVConfigRequestHeader requestHeader = new GetKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(101, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return ((GetKVConfigResponseHeader) response.decodeCommandCustomHeader(GetKVConfigResponseHeader.class)).getValue();
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void putKVConfigValue(String namespace, String key, String value, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        PutKVConfigRequestHeader requestHeader = new PutKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);
        requestHeader.setValue(value);
        RemotingCommand request = RemotingCommand.createRequestCommand(100, requestHeader);
        List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
        if (nameServerAddressList != null) {
            RemotingCommand errResponse = null;
            for (String namesrvAddr : nameServerAddressList) {
                RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, request, timeoutMillis);
                if (assertResult || response != null) {
                    switch (response.getCode()) {
                        case 0:
                            break;
                        default:
                            errResponse = response;
                            break;
                    }
                } else {
                    throw new AssertionError();
                }
            }
            if (errResponse != null) {
                throw new MQClientException(errResponse.getCode(), errResponse.getRemark());
            }
        }
    }

    public void deleteKVConfigValue(String namespace, String key, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        DeleteKVConfigRequestHeader requestHeader = new DeleteKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);
        RemotingCommand request = RemotingCommand.createRequestCommand(102, requestHeader);
        List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
        if (nameServerAddressList != null) {
            RemotingCommand errResponse = null;
            for (String namesrvAddr : nameServerAddressList) {
                RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, request, timeoutMillis);
                if (assertResult || response != null) {
                    switch (response.getCode()) {
                        case 0:
                            break;
                        default:
                            errResponse = response;
                            break;
                    }
                } else {
                    throw new AssertionError();
                }
            }
            if (errResponse != null) {
                throw new MQClientException(errResponse.getCode(), errResponse.getRemark());
            }
        }
    }

    public KVTable getKVListByNamespace(String namespace, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        GetKVListByNamespaceRequestHeader requestHeader = new GetKVListByNamespaceRequestHeader();
        requestHeader.setNamespace(namespace);
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_KVLIST_BY_NAMESPACE, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return (KVTable) KVTable.decode(response.getBody(), KVTable.class);
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public Map<MessageQueue, Long> invokeBrokerToResetOffset(String addr, String topic, String group, long timestamp, boolean isForce, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        return invokeBrokerToResetOffset(addr, topic, group, timestamp, isForce, timeoutMillis, false);
    }

    public Map<MessageQueue, Long> invokeBrokerToResetOffset(String addr, String topic, String group, long timestamp, boolean isForce, long timeoutMillis, boolean isC) throws RemotingException, MQClientException, InterruptedException {
        ResetOffsetRequestHeader requestHeader = new ResetOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        requestHeader.setTimestamp(timestamp);
        requestHeader.setForce(isForce);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.INVOKE_BROKER_TO_RESET_OFFSET, requestHeader);
        if (isC) {
            request.setLanguage(LanguageCode.CPP);
        }
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        return ((ResetOffsetBody) ResetOffsetBody.decode(response.getBody(), ResetOffsetBody.class)).getOffsetTable();
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public Map<String, Map<MessageQueue, Long>> invokeBrokerToGetConsumerStatus(String addr, String topic, String group, String clientAddr, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        GetConsumerStatusRequestHeader requestHeader = new GetConsumerStatusRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        requestHeader.setClientAddr(clientAddr);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.INVOKE_BROKER_TO_GET_CONSUMER_STATUS, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        return ((GetConsumerStatusBody) GetConsumerStatusBody.decode(response.getBody(), GetConsumerStatusBody.class)).getConsumerTable();
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public GroupList queryTopicConsumeByWho(String addr, String topic, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        QueryTopicConsumeByWhoRequestHeader requestHeader = new QueryTopicConsumeByWhoRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return (GroupList) GroupList.decode(response.getBody(), GroupList.class);
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public List<QueueTimeSpan> queryConsumeTimeSpan(String addr, String topic, String group, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        QueryConsumeTimeSpanRequestHeader requestHeader = new QueryConsumeTimeSpanRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUME_TIME_SPAN, requestHeader), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return ((QueryConsumeTimeSpanBody) GroupList.decode(response.getBody(), QueryConsumeTimeSpanBody.class)).getConsumeTimeSpanSet();
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public TopicList getTopicsByCluster(String cluster, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        GetTopicsByClusterRequestHeader requestHeader = new GetTopicsByClusterRequestHeader();
        requestHeader.setCluster(cluster);
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_TOPICS_BY_CLUSTER, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (TopicList) TopicList.decode(body, TopicList.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public void registerMessageFilterClass(String addr, String consumerGroup, String topic, String className, int classCRC, byte[] classBody, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        RegisterMessageFilterClassRequestHeader requestHeader = new RegisterMessageFilterClassRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClassName(className);
        requestHeader.setTopic(topic);
        requestHeader.setClassCRC(Integer.valueOf(classCRC));
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_MESSAGE_FILTER_CLASS, requestHeader);
        request.setBody(classBody);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return;
            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
    }

    public TopicList getSystemTopicList(long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_SYSTEM_TOPIC_LIST_FROM_NS, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        TopicList topicList = (TopicList) TopicList.decode(response.getBody(), TopicList.class);
                        if (topicList.getTopicList() != null && !topicList.getTopicList().isEmpty() && !UtilAll.isBlank(topicList.getBrokerAddr())) {
                            TopicList tmp = getSystemTopicListFromBroker(topicList.getBrokerAddr(), timeoutMillis);
                            if (tmp.getTopicList() != null && !tmp.getTopicList().isEmpty()) {
                                topicList.getTopicList().addAll(tmp.getTopicList());
                            }
                        }
                        return topicList;
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public TopicList getSystemTopicListFromBroker(String addr, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.GET_SYSTEM_TOPIC_LIST_FROM_BROKER, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (TopicList) TopicList.decode(body, TopicList.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public boolean cleanExpiredConsumeQueue(String addr, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.CLEAN_EXPIRED_CONSUMEQUEUE, null), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return true;
            default:
                throw new MQClientException(response.getCode(), response.getRemark());
        }
    }

    public boolean cleanUnusedTopicByAddr(String addr, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.CLEAN_UNUSED_TOPIC, null), timeoutMillis);
        switch (response.getCode()) {
            case 0:
                return true;
            default:
                throw new MQClientException(response.getCode(), response.getRemark());
        }
    }

    public ConsumerRunningInfo getConsumerRunningInfo(String addr, String consumerGroup, String clientId, boolean jstack, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        GetConsumerRunningInfoRequestHeader requestHeader = new GetConsumerRunningInfoRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClientId(clientId);
        requestHeader.setJstackEnable(jstack);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_RUNNING_INFO, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (ConsumerRunningInfo) ConsumerRunningInfo.decode(body, ConsumerRunningInfo.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public ConsumeMessageDirectlyResult consumeMessageDirectly(String addr, String consumerGroup, String clientId, String msgId, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        ConsumeMessageDirectlyResultRequestHeader requestHeader = new ConsumeMessageDirectlyResultRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClientId(clientId);
        requestHeader.setMsgId(msgId);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.CONSUME_MESSAGE_DIRECTLY, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (ConsumeMessageDirectlyResult) ConsumeMessageDirectlyResult.decode(body, ConsumeMessageDirectlyResult.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public Map<Integer, Long> queryCorrectionOffset(String addr, String topic, String group, Set<String> filterGroup, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        QueryCorrectionOffsetHeader requestHeader = new QueryCorrectionOffsetHeader();
        requestHeader.setCompareGroup(group);
        requestHeader.setTopic(topic);
        if (filterGroup != null) {
            StringBuilder sb = new StringBuilder();
            String splitor = "";
            for (String s : filterGroup) {
                sb.append(splitor).append(s);
                splitor = StringArrayPropertyEditor.DEFAULT_SEPARATOR;
            }
            requestHeader.setFilterGroups(sb.toString());
        }
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.QUERY_CORRECTION_OFFSET, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        return ((QueryCorrectionOffsetBody) QueryCorrectionOffsetBody.decode(response.getBody(), QueryCorrectionOffsetBody.class)).getCorrectionOffsets();
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public TopicList getUnitTopicList(boolean containRetry, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_UNIT_TOPIC_LIST, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        TopicList topicList = (TopicList) TopicList.decode(response.getBody(), TopicList.class);
                        if (!containRetry) {
                            Iterator<String> it = topicList.getTopicList().iterator();
                            while (it.hasNext()) {
                                if (it.next().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                                    it.remove();
                                }
                            }
                        }
                        return topicList;
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public TopicList getHasUnitSubTopicList(boolean containRetry, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_HAS_UNIT_SUB_TOPIC_LIST, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        TopicList topicList = (TopicList) TopicList.decode(response.getBody(), TopicList.class);
                        if (!containRetry) {
                            Iterator<String> it = topicList.getTopicList().iterator();
                            while (it.hasNext()) {
                                if (it.next().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                                    it.remove();
                                }
                            }
                        }
                        return topicList;
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public TopicList getHasUnitSubUnUnitTopicList(boolean containRetry, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        RemotingCommand response = this.remotingClient.invokeSync(null, RemotingCommand.createRequestCommand(RequestCode.GET_HAS_UNIT_SUB_UNUNIT_TOPIC_LIST, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    if (response.getBody() != null) {
                        TopicList topicList = (TopicList) TopicList.decode(response.getBody(), TopicList.class);
                        if (!containRetry) {
                            Iterator<String> it = topicList.getTopicList().iterator();
                            while (it.hasNext()) {
                                if (it.next().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                                    it.remove();
                                }
                            }
                        }
                        return topicList;
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public void cloneGroupOffset(String addr, String srcGroup, String destGroup, String topic, boolean isOffline, long timeoutMillis) throws RemotingException, MQClientException, InterruptedException {
        CloneGroupOffsetRequestHeader requestHeader = new CloneGroupOffsetRequestHeader();
        requestHeader.setSrcGroup(srcGroup);
        requestHeader.setDestGroup(destGroup);
        requestHeader.setTopic(topic);
        requestHeader.setOffline(isOffline);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(RequestCode.CLONE_GROUP_OFFSET, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return;
                default:
                    throw new MQClientException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        ViewBrokerStatsDataRequestHeader requestHeader = new ViewBrokerStatsDataRequestHeader();
        requestHeader.setStatsName(statsName);
        requestHeader.setStatsKey(statsKey);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), RemotingCommand.createRequestCommand(RequestCode.VIEW_BROKER_STATS_DATA, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (BrokerStatsData) BrokerStatsData.decode(body, BrokerStatsData.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public Set<String> getClusterList(String topic, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        return Collections.EMPTY_SET;
    }

    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws MQClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        GetConsumeStatsInBrokerHeader requestHeader = new GetConsumeStatsInBrokerHeader();
        requestHeader.setIsOrder(isOrder);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONSUME_STATS, requestHeader), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    byte[] body = response.getBody();
                    if (body != null) {
                        return (ConsumeStatsList) ConsumeStatsList.decode(body, ConsumeStatsList.class);
                    }
                    break;
            }
            throw new MQClientException(response.getCode(), response.getRemark());
        }
        throw new AssertionError();
    }

    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), RemotingCommand.createRequestCommand(201, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return (SubscriptionGroupWrapper) SubscriptionGroupWrapper.decode(response.getBody(), SubscriptionGroupWrapper.class);
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public TopicConfigSerializeWrapper getAllTopicConfig(String addr, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), RemotingCommand.createRequestCommand(21, null), timeoutMillis);
        if (assertResult || response != null) {
            switch (response.getCode()) {
                case 0:
                    return (TopicConfigSerializeWrapper) TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
                default:
                    throw new MQBrokerException(response.getCode(), response.getRemark());
            }
        } else {
            throw new AssertionError();
        }
    }

    public void updateNameServerConfig(Properties properties, List<String> nameServers, long timeoutMillis) throws UnsupportedEncodingException, MQBrokerException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        String str = MixAll.properties2String(properties);
        if (str != null && str.length() >= 1) {
            List<String> invokeNameServers = (nameServers == null || nameServers.isEmpty()) ? this.remotingClient.getNameServerAddressList() : nameServers;
            if (!(invokeNameServers == null || invokeNameServers.isEmpty())) {
                RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_NAMESRV_CONFIG, null);
                request.setBody(str.getBytes("UTF-8"));
                RemotingCommand errResponse = null;
                for (String nameServer : invokeNameServers) {
                    RemotingCommand response = this.remotingClient.invokeSync(nameServer, request, timeoutMillis);
                    if (assertResult || response != null) {
                        switch (response.getCode()) {
                            case 0:
                                break;
                            default:
                                errResponse = response;
                                break;
                        }
                    } else {
                        throw new AssertionError();
                    }
                }
                if (errResponse != null) {
                    throw new MQClientException(errResponse.getCode(), errResponse.getRemark());
                }
            }
        }
    }

    public Map<String, Properties> getNameServerConfig(List<String> nameServers, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException, UnsupportedEncodingException {
        List<String> invokeNameServers = (nameServers == null || nameServers.isEmpty()) ? this.remotingClient.getNameServerAddressList() : nameServers;
        if (invokeNameServers == null || invokeNameServers.isEmpty()) {
            return null;
        }
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_NAMESRV_CONFIG, null);
        Map<String, Properties> configMap = new HashMap<>(4);
        for (String nameServer : invokeNameServers) {
            RemotingCommand response = this.remotingClient.invokeSync(nameServer, request, timeoutMillis);
            if (!assertResult && response == null) {
                throw new AssertionError();
            } else if (0 == response.getCode()) {
                configMap.put(nameServer, MixAll.string2Properties(new String(response.getBody(), "UTF-8")));
            } else {
                throw new MQClientException(response.getCode(), response.getRemark());
            }
        }
        return configMap;
    }

    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic, int queueId, long index, int count, String consumerGroup, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        QueryConsumeQueueRequestHeader requestHeader = new QueryConsumeQueueRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        requestHeader.setIndex(index);
        requestHeader.setCount(count);
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUME_QUEUE, requestHeader), timeoutMillis);
        if (!assertResult && response == null) {
            throw new AssertionError();
        } else if (0 == response.getCode()) {
            return (QueryConsumeQueueResponseBody) QueryConsumeQueueResponseBody.decode(response.getBody(), QueryConsumeQueueResponseBody.class);
        } else {
            throw new MQClientException(response.getCode(), response.getRemark());
        }
    }

    public void checkClientInBroker(String brokerAddr, String consumerGroup, String clientId, SubscriptionData subscriptionData, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        RemotingCommand request = RemotingCommand.createRequestCommand(46, null);
        CheckClientRequestBody requestBody = new CheckClientRequestBody();
        requestBody.setClientId(clientId);
        requestBody.setGroup(consumerGroup);
        requestBody.setSubscriptionData(subscriptionData);
        request.setBody(requestBody.encode());
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);
        if (!assertResult && response == null) {
            throw new AssertionError();
        } else if (0 != response.getCode()) {
            throw new MQClientException(response.getCode(), response.getRemark());
        }
    }
}
