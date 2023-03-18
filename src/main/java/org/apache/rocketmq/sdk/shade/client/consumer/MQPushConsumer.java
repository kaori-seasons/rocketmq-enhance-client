package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListener;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import  org.apache.rocketmq.sdk.api.MessageSelector;
public interface MQPushConsumer extends MQConsumer {
    void start() throws MQClientException;

    void shutdown();

    @Deprecated
    void registerMessageListener(MessageListener messageListener);

    void registerMessageListener(MessageListenerConcurrently messageListenerConcurrently);

    void registerMessageListener(MessageListenerOrderly messageListenerOrderly);

    void subscribe(String str, String str2) throws MQClientException;

    void subscribe(String str, String str2, String str3) throws MQClientException;

    void subscribe(String str, MessageSelector messageSelector) throws MQClientException;

    void unsubscribe(String str);

    void updateCorePoolSize(int i);

    void suspend();

    void resume();
}
