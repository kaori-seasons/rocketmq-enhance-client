package org.apache.rocketmq.sdk.api.order;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.admin.Admin;

public interface OrderProducer extends Admin {
    SendResult send(Message message, String str);
}
