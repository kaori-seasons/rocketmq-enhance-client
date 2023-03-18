package org.apache.rocketmq.sdk.api;

public interface MessageListener {
    Action consume(Message message, ConsumeContext consumeContext);
}
