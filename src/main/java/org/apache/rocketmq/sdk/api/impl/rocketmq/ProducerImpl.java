package org.apache.rocketmq.sdk.api.impl.rocketmq;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.OnExceptionContext;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendCallback;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.tracehook.RMQClientSendMessageHookImpl;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceConstants;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDispatcherType;
import org.apache.rocketmq.sdk.trace.core.dispatch.impl.AsyncArrayDispatcher;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.StringUtils;

public class ProducerImpl extends RMQClientAbstract implements Producer {
    private static final InternalLogger log = ClientLoggerUtil.getClientLogger();
    private final DefaultMQProducer defaultMQProducer;

    public ProducerImpl(Properties properties) {
        super(properties);
        String producerGroup = properties.getProperty(PropertyKeyConst.GROUP_ID, properties.getProperty(PropertyKeyConst.ProducerId));
        producerGroup = StringUtils.isEmpty(producerGroup) ? "__TUXE_PRODUCER_DEFAULT_GROUP" : producerGroup;
        this.defaultMQProducer = new DefaultMQProducer(getNamespace(), producerGroup, Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired)) ? new RMQClientRPCHook(this.sessionCredentials) : null);
        this.defaultMQProducer.setProducerGroup(producerGroup);
        this.defaultMQProducer.setVipChannelEnabled(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isVipChannelEnabled, "false")));
        this.defaultMQProducer.setUseTLS(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isTlsEnable, "false")));
        if (properties.containsKey(PropertyKeyConst.SendMsgTimeoutMillis)) {
            this.defaultMQProducer.setSendMsgTimeout(Integer.valueOf(properties.get(PropertyKeyConst.SendMsgTimeoutMillis).toString()).intValue());
        } else {
            this.defaultMQProducer.setSendMsgTimeout(ErrorCode.UNION);
        }
        if (properties.containsKey(PropertyKeyConst.EXACTLYONCE_DELIVERY)) {
            this.defaultMQProducer.setAddExtendUniqInfo(Boolean.valueOf(properties.get(PropertyKeyConst.EXACTLYONCE_DELIVERY).toString()).booleanValue());
        }
        this.defaultMQProducer.setInstanceName(properties.getProperty("InstanceName", buildInstanceName()));
        if (properties.getProperty("NAMESRV_ADDR") != null) {
            this.defaultMQProducer.setNamesrvAddr(getNameServerAddr());
        } else {
            this.defaultMQProducer.setProxyAddr(getProxyAddr());
        }
        this.defaultMQProducer.setMaxMessageSize(4194304);
        String msgTraceSwitch = properties.getProperty(PropertyKeyConst.MsgTraceSwitch);
        if (UtilAll.isBlank(msgTraceSwitch) || Boolean.parseBoolean(msgTraceSwitch)) {
            try {
                Properties tempProperties = new Properties();
                if (Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired))) {
                    tempProperties.put("AccessKey", this.sessionCredentials.getAccessKey());
                    tempProperties.put("SecretKey", this.sessionCredentials.getSecretKey());
                    tempProperties.put(PropertyKeyConst.AuthenticationRequired, properties.getProperty(PropertyKeyConst.AuthenticationRequired));
                }
                tempProperties.put(RMQTraceConstants.MaxMsgSize, "128000");
                tempProperties.put(RMQTraceConstants.AsyncBufferSize, "2048");
                tempProperties.put(RMQTraceConstants.MaxBatchNum, "100");
                if (null == getNameServerAddr()) {
                    tempProperties.put("PROXY_ADDR", getProxyAddr());
                } else {
                    tempProperties.put("NAMESRV_ADDR", getNameServerAddr());
                }
                tempProperties.put("InstanceName", "PID_CLIENT_INNER_TRACE_PRODUCER");
                tempProperties.put(RMQTraceConstants.TraceDispatcherType, RMQTraceDispatcherType.PRODUCER.name());
                AsyncArrayDispatcher dispatcher = new AsyncArrayDispatcher(tempProperties, this.sessionCredentials);
                dispatcher.setHostProducer(this.defaultMQProducer.getDefaultMQProducerImpl());
                this.traceDispatcher = dispatcher;
                this.defaultMQProducer.getDefaultMQProducerImpl().registerSendMessageHook(new RMQClientSendMessageHookImpl(this.traceDispatcher));
            } catch (Throwable th) {
                log.error("system mqtrace hook init failed ,maybe can't send msg trace data");
            }
        } else {
            log.info("MQ Client Disable the Trace Hook!");
        }
    }

    @Override
    protected void updateNameServerAddr(String newAddrs) {
        this.defaultMQProducer.getDefaultMQProducerImpl().getmQClientFactory().getMQClientAPIImpl().updateNameServerAddressList(newAddrs);
    }

    @Override
    protected void updateProxyAddr(String newAddrs) {
        this.defaultMQProducer.getDefaultMQProducerImpl().getmQClientFactory().getMQClientAPIImpl().updateProxyAddressList(newAddrs);
    }

    @Override
    public void start() {
        try {
            if (this.started.compareAndSet(false, true)) {
                this.defaultMQProducer.start();
                super.start();
            }
        } catch (Exception e) {
            throw new RMQClientException(e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (this.started.compareAndSet(true, false)) {
            this.defaultMQProducer.shutdown();
        }
        super.shutdown();
    }

    @Override
    public SendResult send(Message message) {
        checkONSProducerServiceState(this.defaultMQProducer.getDefaultMQProducerImpl());
        try {
            org.apache.rocketmq.sdk.shade.client.producer.SendResult sendResultRMQ = this.defaultMQProducer.send(RMQUtil.msgConvert(message));
            message.setMsgID(sendResultRMQ.getMsgId());
            SendResult sendResult = new SendResult();
            sendResult.setTopic(sendResultRMQ.getMessageQueue().getTopic());
            sendResult.setMessageId(sendResultRMQ.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error(String.format("Send message Exception, %s", message), (Throwable) e);
            throw checkProducerException(message.getTopic(), message.getMsgID(), e);
        }
    }

    @Override
    public void sendOneway(Message message) {
        checkONSProducerServiceState(this.defaultMQProducer.getDefaultMQProducerImpl());
        org.apache.rocketmq.sdk.shade.common.message.Message msgRMQ = RMQUtil.msgConvert(message);
        try {
            this.defaultMQProducer.sendOneway(msgRMQ);
            message.setMsgID(MessageClientIDSetter.getUniqID(msgRMQ));
        } catch (Exception e) {
            log.error(String.format("Send message oneway Exception, %s", message), (Throwable) e);
            throw checkProducerException(message.getTopic(), message.getMsgID(), e);
        }
    }

    @Override
    public void sendAsync(Message message, SendCallback sendCallback) {
        checkONSProducerServiceState(this.defaultMQProducer.getDefaultMQProducerImpl());
        org.apache.rocketmq.sdk.shade.common.message.Message msgRMQ = RMQUtil.msgConvert(message);
        try {
            this.defaultMQProducer.send(msgRMQ, sendCallbackConvert(message, sendCallback));
            message.setMsgID(MessageClientIDSetter.getUniqID(msgRMQ));
        } catch (Exception e) {
            log.error(String.format("Send message async Exception, %s", message), (Throwable) e);
            throw checkProducerException(message.getTopic(), message.getMsgID(), e);
        }
    }

    @Override
    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        this.defaultMQProducer.setCallbackExecutor(callbackExecutor);
    }

    public DefaultMQProducer getDefaultMQProducer() {
        return this.defaultMQProducer;
    }

    private org.apache.rocketmq.sdk.shade.client.producer.SendCallback sendCallbackConvert(final Message message, final SendCallback sendCallback) {
        return new org.apache.rocketmq.sdk.shade.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.sdk.shade.client.producer.SendResult sendResult) {
                sendCallback.onSuccess(ProducerImpl.this.sendResultConvert(sendResult));
            }

            @Override
            public void onException(Throwable e) {
                String topic = message.getTopic();
                String msgId = message.getMsgID();
                RMQClientException onsEx = ProducerImpl.this.checkProducerException(topic, msgId, e);
                OnExceptionContext context = new OnExceptionContext();
                context.setTopic(topic);
                context.setMessageId(msgId);
                context.setException(onsEx);
                sendCallback.onException(context);
            }
        };
    }

    public SendResult sendResultConvert(org.apache.rocketmq.sdk.shade.client.producer.SendResult rmqSendResult) {
        SendResult sendResult = new SendResult();
        sendResult.setTopic(rmqSendResult.getMessageQueue().getTopic());
        sendResult.setMessageId(rmqSendResult.getMsgId());
        return sendResult;
    }

    public RMQClientException checkProducerException(String topic, String msgId, Throwable e) {
        if (e != null && (e instanceof MQClientException)) {
            if (e.getCause() == null) {
                MQClientException excep = (MQClientException) e;
                if (-1 == excep.getResponseCode()) {
                    return new RMQClientException(FAQ.errorMessage(String.format("Topic does not exist, Topic=%s, msgId=%s", topic, msgId), "http://#####"));
                }
                if (13 == excep.getResponseCode()) {
                    return new RMQClientException(FAQ.errorMessage(String.format("ONS Client check message exception, Topic=%s, msgId=%s", topic, msgId), "http://#####"));
                }
            } else if (e.getCause() instanceof RemotingConnectException) {
                return new RMQClientException(FAQ.errorMessage(String.format("Connect broker failed, Topic=%s, msgId=%s", topic, msgId), "http://#####"));
            } else {
                if (e.getCause() instanceof RemotingTimeoutException) {
                    return new RMQClientException(FAQ.errorMessage(String.format("Send message to broker timeout, %dms, Topic=%s, msgId=%s", Integer.valueOf(this.defaultMQProducer.getSendMsgTimeout()), topic, msgId), "http://#####"));
                }
                if (e.getCause() instanceof MQBrokerException) {
                    return new RMQClientException(FAQ.errorMessage(String.format("Receive a broker exception, Topi=%s, msgId=%s, %s", topic, msgId, ((MQBrokerException) e.getCause()).getErrorMessage()), "http://#####"));
                }
            }
        }
        return new RMQClientException("defaultMQProducer send exception", e);
    }
}
