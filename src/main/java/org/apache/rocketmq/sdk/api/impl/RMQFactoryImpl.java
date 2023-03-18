package org.apache.rocketmq.sdk.api.impl;

import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.RMQFactoryAPI;
import org.apache.rocketmq.sdk.api.batch.BatchConsumer;
import org.apache.rocketmq.sdk.api.impl.rocketmq.BatchConsumerImpl;
import org.apache.rocketmq.sdk.api.impl.rocketmq.ConsumerImpl;
import org.apache.rocketmq.sdk.api.impl.rocketmq.OrderConsumerImpl;
import org.apache.rocketmq.sdk.api.impl.rocketmq.OrderProducerImpl;
import org.apache.rocketmq.sdk.api.impl.rocketmq.ProducerImpl;
import org.apache.rocketmq.sdk.api.impl.rocketmq.TransactionProducerImpl;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import org.apache.rocketmq.sdk.api.transaction.LocalTransactionChecker;
import org.apache.rocketmq.sdk.api.transaction.TransactionProducer;
import org.apache.rocketmq.sdk.api.transaction.TransactionStatus;
import org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionState;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionCheckListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.Properties;

public class RMQFactoryImpl implements RMQFactoryAPI {
    @Override 
    public Producer createProducer(Properties properties) {
        return new ProducerImpl(RMQUtil.extractProperties(properties));
    }

    @Override 
    public Consumer createConsumer(Properties properties) {
        return new ConsumerImpl(RMQUtil.extractProperties(properties));
    }

    @Override 
    public BatchConsumer createBatchConsumer(Properties properties) {
        return new BatchConsumerImpl(RMQUtil.extractProperties(properties));
    }

    @Override 
    public OrderProducer createOrderProducer(Properties properties) {
        return new OrderProducerImpl(RMQUtil.extractProperties(properties));
    }

    @Override 
    public OrderConsumer createOrderedConsumer(Properties properties) {
        return new OrderConsumerImpl(RMQUtil.extractProperties(properties));
    }

    @Override 
    public TransactionProducer createTransactionProducer(Properties properties, final LocalTransactionChecker checker) {
        return new TransactionProducerImpl(RMQUtil.extractProperties(properties), new TransactionCheckListener() {
            @Override
            public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
                String msgId = msg.getProperty(Constants.TRANSACTION_ID);
                Message message = RMQUtil.msgConvert(msg);
                message.setMsgID(msgId);
                TransactionStatus check = checker.check(message);
                if (TransactionStatus.Commit == check) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                if (TransactionStatus.Rollback == check) {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.UNKNOW;
            }
        });
    }

    @Override 
    public TransactionProducer createTransactionProducer(Properties properties, TransactionListener transactionListener) {
        return new TransactionProducerImpl(RMQUtil.extractProperties(properties), transactionListener);
    }
}
