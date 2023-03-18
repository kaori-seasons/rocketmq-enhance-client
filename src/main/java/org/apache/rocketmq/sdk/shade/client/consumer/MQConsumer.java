package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.client.MQAdmin;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.Set;

public interface MQConsumer extends MQAdmin {
    @Deprecated
    void sendMessageBack(MessageExt messageExt, int i) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;

    void sendMessageBack(MessageExt messageExt, int i, String str) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;

    Set<MessageQueue> fetchSubscribeMessageQueues(String str) throws MQClientException;
}
