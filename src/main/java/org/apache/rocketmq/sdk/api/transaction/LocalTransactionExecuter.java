package org.apache.rocketmq.sdk.api.transaction;

import org.apache.rocketmq.sdk.api.Message;

@Deprecated
public interface LocalTransactionExecuter {
    TransactionStatus execute(Message message, Object obj);
}
