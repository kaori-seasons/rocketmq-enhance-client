package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.tracehook.RMQClientSendMessageHookImpl;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.MessageQueueSelector;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceConstants;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDispatcherType;
import org.apache.rocketmq.sdk.trace.core.dispatch.impl.AsyncArrayDispatcher;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

public class OrderProducerImpl extends RMQClientAbstract implements OrderProducer {
    private static final InternalLogger log = ClientLoggerUtil.getClientLogger();
    private final DefaultMQProducer defaultMQProducer;

    public OrderProducerImpl(Properties properties) {
        super(properties);
        String producerGroup = properties.getProperty(PropertyKeyConst.GROUP_ID, properties.getProperty(PropertyKeyConst.ProducerId));
        producerGroup = StringUtils.isEmpty(producerGroup) ? "__TUXE_PRODUCER_DEFAULT_GROUP" : producerGroup;
        this.defaultMQProducer = new DefaultMQProducer(getNamespace(), producerGroup, Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired)) ? new RMQClientRPCHook(this.sessionCredentials) : null);
        this.defaultMQProducer.setProducerGroup(producerGroup);
        this.defaultMQProducer.setVipChannelEnabled(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isVipChannelEnabled, "false")));
        this.defaultMQProducer.setSendMsgTimeout(Integer.parseInt(properties.getProperty(PropertyKeyConst.SendMsgTimeoutMillis, "10000")));
        this.defaultMQProducer.setAddExtendUniqInfo(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "false")));
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
    public SendResult send(Message message, String shardingKey) {
        if (UtilAll.isBlank(shardingKey)) {
            throw new RMQClientException("'shardingKey' is blank.");
        }
        message.setShardingKey(shardingKey);
        checkONSProducerServiceState(this.defaultMQProducer.getDefaultMQProducerImpl());
        try {
            org.apache.rocketmq.sdk.shade.client.producer.SendResult sendResultRMQ = this.defaultMQProducer.send(RMQUtil.msgConvert(message), new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, org.apache.rocketmq.sdk.shade.common.message.Message msg, Object shardingKey2) {
                    int select = Math.abs(shardingKey2.hashCode());
                    if (select < 0) {
                        select = 0;
                    }
                    return mqs.get(select % mqs.size());
                }
            }, shardingKey);
            message.setMsgID(sendResultRMQ.getMsgId());
            SendResult sendResult = new SendResult();
            sendResult.setTopic(message.getTopic());
            sendResult.setMessageId(sendResultRMQ.getMsgId());
            return sendResult;
        } catch (Exception e) {
            throw new RMQClientException("defaultMQProducer send order exception", e);
        }
    }
}
