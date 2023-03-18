package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;

import java.util.concurrent.ExecutorService;

public class TransactionMQProducer extends DefaultMQProducer {
    private TransactionCheckListener transactionCheckListener;
    private int checkThreadPoolMinSize = 1;
    private int checkThreadPoolMaxSize = 1;
    private int checkRequestHoldMax = 2000;
    private ExecutorService executorService;
    private TransactionListener transactionListener;

    public TransactionMQProducer() {
    }

    public TransactionMQProducer(String producerGroup) {
        super(producerGroup);
    }

    public TransactionMQProducer(String producerGroup, RPCHook rpcHook) {
        super(null, producerGroup, rpcHook);
    }

    public TransactionMQProducer(String namespace, String producerGroup, RPCHook rpcHook) {
        super(namespace, producerGroup, rpcHook);
    }

    @Override 
    public void start() throws MQClientException {
        this.defaultMQProducerImpl.initTransactionEnv();
        super.start();
    }

    @Override 
    public void shutdown() {
        super.shutdown();
        this.defaultMQProducerImpl.destroyTransactionEnv();
    }

    @Override 
    @Deprecated
    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecuter tranExecuter, Object arg) throws MQClientException {
        if (null == this.transactionCheckListener) {
            throw new MQClientException("localTransactionBranchCheckListener is null", (Throwable) null);
        }
        msg.setTopic(NamespaceUtil.wrapNamespace(getNamespace(), msg.getTopic()));
        return this.defaultMQProducerImpl.sendMessageInTransaction(msg, tranExecuter, arg);
    }

    @Override 
    public TransactionSendResult sendMessageInTransaction(Message msg, Object arg) throws MQClientException {
        if (null == this.transactionListener) {
            throw new MQClientException("TransactionListener is null", (Throwable) null);
        }
        msg.setTopic(NamespaceUtil.wrapNamespace(getNamespace(), msg.getTopic()));
        return this.defaultMQProducerImpl.sendMessageInTransaction(msg, null, arg);
    }

    public TransactionCheckListener getTransactionCheckListener() {
        return this.transactionCheckListener;
    }

    @Deprecated
    public void setTransactionCheckListener(TransactionCheckListener transactionCheckListener) {
        this.transactionCheckListener = transactionCheckListener;
    }

    public int getCheckThreadPoolMinSize() {
        return this.checkThreadPoolMinSize;
    }

    @Deprecated
    public void setCheckThreadPoolMinSize(int checkThreadPoolMinSize) {
        this.checkThreadPoolMinSize = checkThreadPoolMinSize;
    }

    public int getCheckThreadPoolMaxSize() {
        return this.checkThreadPoolMaxSize;
    }

    @Deprecated
    public void setCheckThreadPoolMaxSize(int checkThreadPoolMaxSize) {
        this.checkThreadPoolMaxSize = checkThreadPoolMaxSize;
    }

    public int getCheckRequestHoldMax() {
        return this.checkRequestHoldMax;
    }

    @Deprecated
    public void setCheckRequestHoldMax(int checkRequestHoldMax) {
        this.checkRequestHoldMax = checkRequestHoldMax;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public TransactionListener getTransactionListener() {
        return this.transactionListener;
    }

    public void setTransactionListener(TransactionListener transactionListener) {
        this.transactionListener = transactionListener;
    }
}
