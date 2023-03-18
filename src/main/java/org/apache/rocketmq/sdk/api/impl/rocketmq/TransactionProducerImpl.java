package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.impl.tracehook.RMQClientSendMessageHookImpl;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.api.transaction.LocalTransactionExecuter;
import org.apache.rocketmq.sdk.api.transaction.TransactionProducer;
import org.apache.rocketmq.sdk.api.transaction.TransactionStatus;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionState;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionCheckListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionSendResult;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceConstants;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceDispatcherType;
import org.apache.rocketmq.sdk.trace.core.dispatch.impl.AsyncArrayDispatcher;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

public class TransactionProducerImpl extends RMQClientAbstract implements TransactionProducer {
    private static final InternalLogger log = ClientLoggerUtil.getClientLogger();
    TransactionMQProducer transactionMQProducer;
    private Properties properties;
    private TransactionListener transactionListener;

    @Deprecated
    public TransactionProducerImpl(Properties properties, TransactionCheckListener transactionCheckListener) {
        super(properties);
        this.transactionMQProducer = null;
        this.properties = properties;
        this.transactionMQProducer = new TransactionMQProducer((String) properties.get(PropertyKeyConst.ProducerId), Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired)) ? new RMQClientRPCHook(this.sessionCredentials) : null);
        this.transactionMQProducer.setVipChannelEnabled(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isVipChannelEnabled, "false")));
        this.transactionMQProducer.setInstanceName(properties.getProperty("InstanceName", buildInstanceName()));
        this.transactionMQProducer.setTransactionCheckListener(transactionCheckListener);
    }

    public TransactionProducerImpl(Properties properties, TransactionListener transactionListener) {
        super(properties);
        this.transactionMQProducer = null;
        this.properties = properties;
        String producerGroup = properties.getProperty(PropertyKeyConst.GROUP_ID, properties.getProperty(PropertyKeyConst.ProducerId));
        this.transactionMQProducer = new TransactionMQProducer(getNamespace(), StringUtils.isEmpty(producerGroup) ? "__TUXE_PRODUCER_DEFAULT_GROUP" : producerGroup, Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired)) ? new RMQClientRPCHook(this.sessionCredentials) : null);
        this.transactionMQProducer.setVipChannelEnabled(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.isVipChannelEnabled, "false")));
        this.transactionMQProducer.setSendMsgTimeout(Integer.parseInt(properties.getProperty(PropertyKeyConst.SendMsgTimeoutMillis, "10000")));
        this.transactionMQProducer.setInstanceName(properties.getProperty("InstanceName", buildInstanceName()));
        this.transactionListener = transactionListener;
        this.transactionMQProducer.setAddExtendUniqInfo(Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "false")));
        this.transactionMQProducer.setTransactionListener(transactionListener);
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
                dispatcher.setHostProducer(this.transactionMQProducer.getDefaultMQProducerImpl());
                this.traceDispatcher = dispatcher;
                this.transactionMQProducer.getDefaultMQProducerImpl().registerSendMessageHook(new RMQClientSendMessageHookImpl(this.traceDispatcher));
            } catch (Throwable th) {
                log.error("system mqtrace hook init failed ,maybe can't send msg trace data");
            }
        } else {
            log.info("MQ Client Disable the Trace Hook!");
        }
    }

    @Override
    public void start() {
        if (!this.started.compareAndSet(false, true)) {
            return;
        }
        if (this.transactionMQProducer.getTransactionCheckListener() == null && this.transactionMQProducer.getTransactionListener() == null) {
            throw new IllegalArgumentException("TransactionCheckListener or TransactionListener can not be null at the same time.");
        }
        if (null == this.proxyAddr) {
            this.transactionMQProducer.setNamesrvAddr(this.nameServerAddr);
        }
        if (null == this.nameServerAddr) {
            this.transactionMQProducer.setProxyAddr(this.proxyAddr);
        }
        try {
            this.transactionMQProducer.start();
            super.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateNameServerAddr(String newAddrs) {
        this.transactionMQProducer.getDefaultMQProducerImpl().getmQClientFactory().getMQClientAPIImpl().updateNameServerAddressList(newAddrs);
    }

    @Override
    protected void updateProxyAddr(String newAddrs) {
        this.transactionMQProducer.getDefaultMQProducerImpl().getmQClientFactory().getMQClientAPIImpl().updateProxyAddressList(newAddrs);
    }

    @Override
    public void shutdown() {
        if (this.started.compareAndSet(true, false)) {
            this.transactionMQProducer.shutdown();
        }
        super.shutdown();
    }

    @Override
    @Deprecated
    public SendResult send(final Message message, final LocalTransactionExecuter executer, Object arg) {
        checkONSProducerServiceState(this.transactionMQProducer.getDefaultMQProducerImpl());
        org.apache.rocketmq.sdk.shade.common.message.Message msgRMQ = RMQUtil.msgConvert(message);
        MessageAccessor.putProperty(msgRMQ, PropertyKeyConst.ProducerId, (String) this.properties.get(PropertyKeyConst.ProducerId));
        try {
            TransactionSendResult sendResultRMQ = this.transactionMQProducer.sendMessageInTransaction(msgRMQ, new org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionExecuter() {
                @Override
                public LocalTransactionState executeLocalTransactionBranch(org.apache.rocketmq.sdk.shade.common.message.Message msg, Object arg2) {
                    message.setMsgID(msg.getProperty(Constants.TRANSACTION_ID));
                    TransactionStatus transactionStatus = executer.execute(message, arg2);
                    if (TransactionStatus.Commit == transactionStatus) {
                        return LocalTransactionState.COMMIT_MESSAGE;
                    }
                    if (TransactionStatus.Rollback == transactionStatus) {
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }
                    return LocalTransactionState.UNKNOW;
                }
            }, arg);
            if (sendResultRMQ.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
                throw new RuntimeException("local transaction branch failed ,so transaction rollback");
            }
            SendResult sendResult = new SendResult();
            sendResult.setMessageId(sendResultRMQ.getMsgId());
            sendResult.setTopic(sendResultRMQ.getMessageQueue().getTopic());
            return sendResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SendResult send(Message message, Object arg) {
        checkONSProducerServiceState(this.transactionMQProducer.getDefaultMQProducerImpl());
        org.apache.rocketmq.sdk.shade.common.message.Message msgRMQ = RMQUtil.msgConvert(message);
        MessageAccessor.putProperty(msgRMQ, PropertyKeyConst.ProducerId, (String) this.properties.get(PropertyKeyConst.ProducerId));
        try {
            TransactionSendResult sendResultRMQ = this.transactionMQProducer.sendMessageInTransaction(msgRMQ, arg);
            if (sendResultRMQ.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
                throw new RuntimeException("local transaction branch failed ,so transaction rollback");
            }
            SendResult sendResult = new SendResult();
            sendResult.setMessageId(sendResultRMQ.getMsgId());
            sendResult.setTopic(sendResultRMQ.getMessageQueue().getTopic());
            return sendResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TransactionListener getTransactionListener() {
        return this.transactionListener;
    }

    @Override
    public void setTransactionListener(TransactionListener transactionListener) {
        this.transactionListener = transactionListener;
        this.transactionMQProducer.setTransactionListener(this.transactionListener);
    }
}
