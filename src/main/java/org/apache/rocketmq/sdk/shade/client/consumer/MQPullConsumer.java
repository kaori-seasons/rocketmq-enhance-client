package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.Set;

public interface MQPullConsumer extends MQConsumer {
    void start() throws MQClientException;

    void shutdown();

    void registerMessageQueueListener(String str, MessageQueueListener messageQueueListener);

    PullResult pull(MessageQueue messageQueue, String str, long j, int i) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    PullResult pull(MessageQueue messageQueue, String str, long j, int i, long j2) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void pull(MessageQueue messageQueue, String str, long j, int i, PullCallback pullCallback) throws MQClientException, RemotingException, InterruptedException;

    void pull(MessageQueue messageQueue, String str, long j, int i, PullCallback pullCallback, long j2) throws MQClientException, RemotingException, InterruptedException;

    PullResult pullBlockIfNotFound(MessageQueue messageQueue, String str, long j, int i) throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void pullBlockIfNotFound(MessageQueue messageQueue, String str, long j, int i, PullCallback pullCallback) throws MQClientException, RemotingException, InterruptedException;

    void updateConsumeOffset(MessageQueue messageQueue, long j) throws MQClientException;

    long fetchConsumeOffset(MessageQueue messageQueue, boolean z) throws MQClientException;

    Set<MessageQueue> fetchMessageQueuesInBalance(String str) throws MQClientException;

    void sendMessageBack(MessageExt messageExt, int i, String str, String str2) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;
}
