package org.apache.rocketmq.sdk.shade.client;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

public interface MQAdmin {
    void createTopic(String str, String str2, int i) throws MQClientException;

    void createTopic(String str, String str2, int i, int i2) throws MQClientException;

    long searchOffset(MessageQueue messageQueue, long j) throws MQClientException;

    long maxOffset(MessageQueue messageQueue) throws MQClientException;

    long minOffset(MessageQueue messageQueue) throws MQClientException;

    long earliestMsgStoreTime(MessageQueue messageQueue) throws MQClientException;

    MessageExt viewMessage(String str) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;

    QueryResult queryMessage(String str, String str2, int i, long j, long j2) throws MQClientException, InterruptedException;

    MessageExt viewMessage(String str, String str2) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;
}
