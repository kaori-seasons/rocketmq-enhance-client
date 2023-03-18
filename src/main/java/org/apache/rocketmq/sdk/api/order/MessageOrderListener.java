package org.apache.rocketmq.sdk.api.order;

import org.apache.rocketmq.sdk.api.Message;

public interface MessageOrderListener {
    OrderAction consume(Message message, ConsumeOrderContext consumeOrderContext);
}
