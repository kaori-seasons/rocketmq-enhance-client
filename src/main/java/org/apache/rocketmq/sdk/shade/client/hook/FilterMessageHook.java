package org.apache.rocketmq.sdk.shade.client.hook;

public interface FilterMessageHook {
    String hookName();

    void filterMessage(FilterMessageContext filterMessageContext);
}
