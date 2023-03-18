package org.apache.rocketmq.sdk.shade.client.hook;

public interface SendMessageHook {
    String hookName();

    void sendMessageBefore(SendMessageContext sendMessageContext);

    void sendMessageAfter(SendMessageContext sendMessageContext);
}
