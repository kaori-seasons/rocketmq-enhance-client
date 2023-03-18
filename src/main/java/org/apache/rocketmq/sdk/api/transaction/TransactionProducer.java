package org.apache.rocketmq.sdk.api.transaction;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.admin.Admin;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;

public interface TransactionProducer extends Admin {
    @Override 
    void start();

    @Override 
    void shutdown();

    @Deprecated
    SendResult send(Message message, LocalTransactionExecuter localTransactionExecuter, Object obj);

    SendResult send(Message message, Object obj);

    void setTransactionListener(TransactionListener transactionListener);
}
