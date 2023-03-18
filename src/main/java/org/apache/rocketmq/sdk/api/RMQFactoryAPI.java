package org.apache.rocketmq.sdk.api;

import org.apache.rocketmq.sdk.api.batch.BatchConsumer;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import org.apache.rocketmq.sdk.api.transaction.LocalTransactionChecker;
import org.apache.rocketmq.sdk.api.transaction.TransactionProducer;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;

import java.util.Properties;

public interface RMQFactoryAPI {
    Producer createProducer(Properties properties);

    Consumer createConsumer(Properties properties);

    BatchConsumer createBatchConsumer(Properties properties);

    OrderProducer createOrderProducer(Properties properties);

    OrderConsumer createOrderedConsumer(Properties properties);

    @Deprecated
    TransactionProducer createTransactionProducer(Properties properties, LocalTransactionChecker localTransactionChecker);

    TransactionProducer createTransactionProducer(Properties properties, TransactionListener transactionListener);
}
