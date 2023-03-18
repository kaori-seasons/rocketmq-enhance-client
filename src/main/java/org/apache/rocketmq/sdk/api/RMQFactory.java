package org.apache.rocketmq.sdk.api;

import org.apache.rocketmq.sdk.api.batch.BatchConsumer;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import org.apache.rocketmq.sdk.api.transaction.LocalTransactionChecker;
import org.apache.rocketmq.sdk.api.transaction.TransactionProducer;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;

import java.util.Properties;

public class RMQFactory {
    private static RMQFactoryAPI rmqFactory;

    static {
        rmqFactory = null;
        try {
            rmqFactory = (RMQFactoryAPI) RMQFactory.class.getClassLoader().loadClass("org.apache.rocketmq.sdk.api.impl.RMQFactoryImpl").newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Producer createProducer(Properties properties) {
        return rmqFactory.createProducer(properties);
    }

    public static OrderProducer createOrderProducer(Properties properties) {
        return rmqFactory.createOrderProducer(properties);
    }

    @Deprecated
    public static TransactionProducer createTransactionProducer(Properties properties, LocalTransactionChecker checker) {
        return rmqFactory.createTransactionProducer(properties, checker);
    }

    public static TransactionProducer createTransactionProducer(Properties properties, TransactionListener transactionListener) {
        return rmqFactory.createTransactionProducer(properties, transactionListener);
    }

    public static Consumer createConsumer(Properties properties) {
        return rmqFactory.createConsumer(properties);
    }

    public static BatchConsumer createBatchConsumer(Properties properties) {
        return rmqFactory.createBatchConsumer(properties);
    }

    public static OrderConsumer createOrderedConsumer(Properties properties) {
        return rmqFactory.createOrderedConsumer(properties);
    }
}
