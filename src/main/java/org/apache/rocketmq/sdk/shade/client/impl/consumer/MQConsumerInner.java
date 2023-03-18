package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;

import java.util.Set;

public interface MQConsumerInner {
    String groupName();

    MessageModel messageModel();

    ConsumeType consumeType();

    ConsumeFromWhere consumeFromWhere();

    Set<SubscriptionData> subscriptions();

    void doRebalance();

    void persistConsumerOffset();

    void updateTopicSubscribeInfo(String str, Set<MessageQueue> set);

    boolean isSubscribeTopicNeedUpdate(String str);

    boolean isUnitMode();

    ConsumerRunningInfo consumerRunningInfo();
}
