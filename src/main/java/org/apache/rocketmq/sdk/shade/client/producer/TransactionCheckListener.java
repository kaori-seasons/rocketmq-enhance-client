package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

@Deprecated
public interface TransactionCheckListener {
    LocalTransactionState checkLocalTransactionState(MessageExt messageExt);
}
