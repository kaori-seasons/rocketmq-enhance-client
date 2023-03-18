package org.apache.rocketmq.sdk.shade.client.hook;

import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

public interface CheckSendBackHook {
    String hookName();

    boolean needSendBack(MessageExt messageExt, ConsumeConcurrentlyContext consumeConcurrentlyContext);
}
