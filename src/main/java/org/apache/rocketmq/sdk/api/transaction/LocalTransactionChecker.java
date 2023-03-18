package org.apache.rocketmq.sdk.api.transaction;

import org.apache.rocketmq.sdk.api.Message;

@Deprecated
public interface LocalTransactionChecker {
    TransactionStatus check(Message message);
}
