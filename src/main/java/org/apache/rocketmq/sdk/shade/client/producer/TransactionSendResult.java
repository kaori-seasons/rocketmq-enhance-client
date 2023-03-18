package org.apache.rocketmq.sdk.shade.client.producer;

public class TransactionSendResult extends SendResult {
    private LocalTransactionState localTransactionState;

    public LocalTransactionState getLocalTransactionState() {
        return this.localTransactionState;
    }

    public void setLocalTransactionState(LocalTransactionState localTransactionState) {
        this.localTransactionState = localTransactionState;
    }
}
