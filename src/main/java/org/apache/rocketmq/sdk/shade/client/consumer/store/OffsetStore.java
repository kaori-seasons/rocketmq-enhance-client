package org.apache.rocketmq.sdk.shade.client.consumer.store;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.Map;
import java.util.Set;

public interface OffsetStore {
    void load() throws MQClientException;

    void updateOffset(MessageQueue messageQueue, long j, boolean z);

    long readOffset(MessageQueue messageQueue, ReadOffsetType readOffsetType);

    void persistAll(Set<MessageQueue> set);

    void persist(MessageQueue messageQueue);

    void removeOffset(MessageQueue messageQueue);

    Map<MessageQueue, Long> cloneOffsetTable(String str);

    void updateConsumeOffsetToBroker(MessageQueue messageQueue, long j, boolean z) throws RemotingException, MQBrokerException, InterruptedException, MQClientException;
}
