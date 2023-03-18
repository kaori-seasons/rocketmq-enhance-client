package org.apache.rocketmq.sdk.shade.client.impl.producer;

import org.apache.rocketmq.sdk.shade.client.producer.TransactionCheckListener;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.protocol.header.CheckTransactionStateRequestHeader;

import java.util.Set;

public interface MQProducerInner {
    Set<String> getPublishTopicList();

    boolean isPublishTopicNeedUpdate(String str);

    TransactionCheckListener checkListener();

    TransactionListener getCheckListener();

    void checkTransactionState(String str, MessageExt messageExt, CheckTransactionStateRequestHeader checkTransactionStateRequestHeader);

    void updateTopicPublishInfo(String str, TopicPublishInfo topicPublishInfo);

    boolean isUnitMode();
}
