package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;

public interface AllocateMessageQueueStrategy {
    List<MessageQueue> allocate(String str, String str2, List<MessageQueue> list, List<String> list2);

    String getName();
}
