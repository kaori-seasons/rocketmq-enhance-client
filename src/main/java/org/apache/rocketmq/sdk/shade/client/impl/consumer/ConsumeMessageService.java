package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;

import java.util.List;

public interface ConsumeMessageService {
    void start();

    void shutdown();

    void updateCorePoolSize(int i);

    void incCorePoolSize();

    void decCorePoolSize();

    int getCorePoolSize();

    ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt messageExt, String str);

    void submitConsumeRequest(List<MessageExt> list, ProcessQueue processQueue, MessageQueue messageQueue, boolean z);
}
