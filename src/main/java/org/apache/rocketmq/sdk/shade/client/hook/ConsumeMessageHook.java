package org.apache.rocketmq.sdk.shade.client.hook;

public interface ConsumeMessageHook {
    String hookName();

    void consumeMessageBefore(ConsumeMessageContext consumeMessageContext);

    void consumeMessageAfter(ConsumeMessageContext consumeMessageContext);
}
