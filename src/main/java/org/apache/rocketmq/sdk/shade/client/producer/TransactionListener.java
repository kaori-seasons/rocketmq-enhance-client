package org.apache.rocketmq.sdk.shade.client.producer;

import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

public interface TransactionListener {
    LocalTransactionState executeLocalTransaction(Message message, Object obj);

    LocalTransactionState checkLocalTransaction(MessageExt messageExt);
}
