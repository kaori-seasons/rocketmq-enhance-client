package org.apache.rocketmq.sdk.api.order;

import org.apache.rocketmq.sdk.api.Message;

public interface MessageQueueSelector {
    int select(int i, Message message, Object obj);
}
