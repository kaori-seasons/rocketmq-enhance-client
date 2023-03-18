package org.apache.rocketmq.sdk.api.impl.rocketmq;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.tracehook.RMQClientConsumeMessageHookImpl;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.filter.ExpressionType;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceConstants;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDispatcherType;
import org.apache.rocketmq.sdk.trace.core.dispatch.impl.AsyncArrayDispatcher;
import java.util.Properties;

import org.springframework.asm.Opcodes;

public class RMQConsumerAbstract extends RMQClientAbstract {
    static final InternalLogger log = ClientLoggerUtil.getClientLogger();
    protected final DefaultMQPushConsumer defaultMQPushConsumer;
    private static final int MAX_CACHED_MESSAGE_SIZE_IN_MIB = 2048;
    private static final int MIN_CACHED_MESSAGE_SIZE_IN_MIB = 16;
    private static final int MAX_CACHED_MESSAGE_AMOUNT = 50000;
    private static final int MIN_CACHED_MESSAGE_AMOUNT = 100;
    private int maxCachedMessageSizeInMiB;
    private int maxCachedMessageAmount;

    public RMQConsumerAbstract(Properties properties) {
        super(properties);
        this.maxCachedMessageSizeInMiB = Opcodes.ACC_INTERFACE;
        this.maxCachedMessageAmount = ErrorCode.UNION;
        RMQClientRPCHook RMQClientRPCHook = Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired)) ? new RMQClientRPCHook(this.sessionCredentials) : null;
        String consumerGroup = properties.getProperty(PropertyKeyConst.GROUP_ID, properties.getProperty(PropertyKeyConst.ConsumerId));
        if (null == consumerGroup) {
            throw new RMQClientException("ConsumerId property is null");
        }
        this.defaultMQPushConsumer = new DefaultMQPushConsumer(getNamespace(), consumerGroup, RMQClientRPCHook);
        String maxReconsumeTimes = properties.getProperty(PropertyKeyConst.MaxReconsumeTimes);
        if (!UtilAll.isBlank(maxReconsumeTimes)) {
            try {
                this.defaultMQPushConsumer.setMaxReconsumeTimes(Integer.parseInt(maxReconsumeTimes));
            } catch (NumberFormatException e) {
            }
        }
        String consumeTimeout = properties.getProperty(PropertyKeyConst.ConsumeTimeout);
        if (!UtilAll.isBlank(consumeTimeout)) {
            try {
                this.defaultMQPushConsumer.setConsumeTimeout((long) Integer.parseInt(consumeTimeout));
            } catch (NumberFormatException e2) {
            }
        }
        this.defaultMQPushConsumer.setVipChannelEnabled(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isVipChannelEnabled, "false")));
        this.defaultMQPushConsumer.setConsumerGroup(consumerGroup);
        this.defaultMQPushConsumer.setInstanceName(properties.getProperty("InstanceName", buildInstanceName()));
        if (properties.getProperty("NAMESRV_ADDR") != null) {
            this.defaultMQPushConsumer.setNamesrvAddr(getNameServerAddr());
        } else {
            this.defaultMQPushConsumer.setProxyAddr(getProxyAddr());
        }
        String consumeThreadNums = properties.getProperty(PropertyKeyConst.ConsumeThreadNums);
        if (!UtilAll.isBlank(consumeThreadNums)) {
            this.defaultMQPushConsumer.setConsumeThreadMin(Integer.valueOf(consumeThreadNums).intValue());
            this.defaultMQPushConsumer.setConsumeThreadMax(Integer.valueOf(consumeThreadNums).intValue());
        }
        String configuredCachedMessageAmount = properties.getProperty(PropertyKeyConst.MaxCachedMessageAmount);
        if (!UtilAll.isBlank(configuredCachedMessageAmount)) {
            this.maxCachedMessageAmount = Math.min((int) MAX_CACHED_MESSAGE_AMOUNT, Integer.valueOf(configuredCachedMessageAmount).intValue());
            this.maxCachedMessageAmount = Math.max(100, this.maxCachedMessageAmount);
            this.defaultMQPushConsumer.setPullThresholdForTopic(this.maxCachedMessageAmount);
        }
        String configuredCachedMessageSizeInMiB = properties.getProperty(PropertyKeyConst.MaxCachedMessageSizeInMiB);
        if (!UtilAll.isBlank(configuredCachedMessageSizeInMiB)) {
            this.maxCachedMessageSizeInMiB = Math.min(2048, Integer.valueOf(configuredCachedMessageSizeInMiB).intValue());
            this.maxCachedMessageSizeInMiB = Math.max(16, this.maxCachedMessageSizeInMiB);
            this.defaultMQPushConsumer.setPullThresholdSizeForTopic(this.maxCachedMessageSizeInMiB);
        }
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
                tempProperties.put(RMQTraceConstants.TraceDispatcherType, RMQTraceDispatcherType.CONSUMER.name());
                AsyncArrayDispatcher dispatcher = new AsyncArrayDispatcher(tempProperties, this.sessionCredentials);
                dispatcher.setHostConsumer(this.defaultMQPushConsumer.getDefaultMQPushConsumerImpl());
                this.traceDispatcher = dispatcher;
                this.defaultMQPushConsumer.getDefaultMQPushConsumerImpl().registerConsumeMessageHook(new RMQClientConsumeMessageHookImpl(this.traceDispatcher));
            } catch (Throwable th) {
                log.error("system mqtrace hook init failed ,maybe can't send msg trace data");
            }
        } else {
            log.info("MQ Client Disable the Trace Hook!");
        }
    }

    @Override
    protected void updateNameServerAddr(String newAddrs) {
        this.defaultMQPushConsumer.getDefaultMQPushConsumerImpl().getmQClientFactory().getMQClientAPIImpl().updateNameServerAddressList(newAddrs);
    }

    @Override
    protected void updateProxyAddr(String newAddrs) {
        this.defaultMQPushConsumer.getDefaultMQPushConsumerImpl().getmQClientFactory().getMQClientAPIImpl().updateProxyAddressList(newAddrs);
    }

    public void subscribe(String topic, String subExpression) {
        try {
            this.defaultMQPushConsumer.subscribe(topic, subExpression);
        } catch (MQClientException e) {
            throw new RMQClientException("defaultMQPushConsumer subscribe exception", e);
        }
    }

    public void subscribe(String topic, MessageSelector selector) {
        MessageSelector messageSelector;
        String subExpression = "*";
        String type = ExpressionType.TAG;
        if (selector != null) {
            if (selector.getType() == null) {
                throw new RMQClientException("Expression type is null!");
            }
            subExpression = selector.getSubExpression();
            type = selector.getType().name();
        }
        if (ExpressionType.SQL92.equals(type)) {
            messageSelector = MessageSelector.bySql(subExpression);
        } else if (ExpressionType.TAG.equals(type)) {
            messageSelector = MessageSelector.byTag(subExpression);
        } else {
            throw new RMQClientException(String.format("Expression type %s is unknown!", type));
        }
        try {
            this.defaultMQPushConsumer.subscribe(topic, messageSelector);
        } catch (MQClientException e) {
            throw new RMQClientException("Consumer subscribe exception", e);
        }
    }

    void unsubscribe(String topic) {
        this.defaultMQPushConsumer.unsubscribe(topic);
    }

    @Override
    public void start() {
        try {
            if (this.started.compareAndSet(false, true)) {
                this.defaultMQPushConsumer.start();
                super.start();
            }
        } catch (Exception e) {
            throw new RMQClientException(e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (this.started.compareAndSet(true, false)) {
            this.defaultMQPushConsumer.shutdown();
        }
        super.shutdown();
    }
}
