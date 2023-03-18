package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.client.MQAdmin;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.Collection;
import java.util.List;

public interface MQProducer extends MQAdmin {
    void start() throws MQClientException;

    void shutdown();

    List<MessageQueue> fetchPublishMessageQueues(String str) throws MQClientException;

    SendResult send(Message message) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Message message, long j) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void send(Message message, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException;

    void send(Message message, SendCallback sendCallback, long j) throws MQClientException, RemotingException, InterruptedException;

    void sendOneway(Message message) throws MQClientException, RemotingException, InterruptedException;

    SendResult send(Message message, MessageQueue messageQueue) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Message message, MessageQueue messageQueue, long j) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void send(Message message, MessageQueue messageQueue, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException;

    void send(Message message, MessageQueue messageQueue, SendCallback sendCallback, long j) throws MQClientException, RemotingException, InterruptedException;

    void sendOneway(Message message, MessageQueue messageQueue) throws MQClientException, RemotingException, InterruptedException;

    SendResult send(Message message, MessageQueueSelector messageQueueSelector, Object obj) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Message message, MessageQueueSelector messageQueueSelector, Object obj, long j) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void send(Message message, MessageQueueSelector messageQueueSelector, Object obj, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException;

    void send(Message message, MessageQueueSelector messageQueueSelector, Object obj, SendCallback sendCallback, long j) throws MQClientException, RemotingException, InterruptedException;

    void sendOneway(Message message, MessageQueueSelector messageQueueSelector, Object obj) throws MQClientException, RemotingException, InterruptedException;

    TransactionSendResult sendMessageInTransaction(Message message, LocalTransactionExecuter localTransactionExecuter, Object obj) throws MQClientException;

    TransactionSendResult sendMessageInTransaction(Message message, Object obj) throws MQClientException;

    SendResult send(Collection<Message> collection) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Collection<Message> collection, long j) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Collection<Message> collection, MessageQueue messageQueue) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Collection<Message> collection, MessageQueue messageQueue, long j) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;
}
