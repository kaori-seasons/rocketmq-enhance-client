package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.common.message.Message;

@Deprecated
public interface LocalTransactionExecuter {
    LocalTransactionState executeLocalTransactionBranch(Message message, Object obj);
}
