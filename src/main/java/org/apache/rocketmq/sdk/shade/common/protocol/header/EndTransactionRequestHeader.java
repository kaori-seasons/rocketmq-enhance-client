package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class EndTransactionRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String producerGroup;
    @CFNotNull
    private Long tranStateTableOffset;
    @CFNotNull
    private Long commitLogOffset;
    @CFNotNull
    private Integer commitOrRollback;
    @CFNullable
    private Boolean fromTransactionCheck = false;
    @CFNotNull
    private String msgId;
    private String transactionId;

    @Override
    public void checkFields() throws RemotingCommandException {
        if (0 != this.commitOrRollback.intValue() && 8 != this.commitOrRollback.intValue() && 12 != this.commitOrRollback.intValue()) {
            throw new RemotingCommandException("commitOrRollback field wrong");
        }
    }

    public String getProducerGroup() {
        return this.producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public Long getTranStateTableOffset() {
        return this.tranStateTableOffset;
    }

    public void setTranStateTableOffset(Long tranStateTableOffset) {
        this.tranStateTableOffset = tranStateTableOffset;
    }

    public Long getCommitLogOffset() {
        return this.commitLogOffset;
    }

    public void setCommitLogOffset(Long commitLogOffset) {
        this.commitLogOffset = commitLogOffset;
    }

    public Integer getCommitOrRollback() {
        return this.commitOrRollback;
    }

    public void setCommitOrRollback(Integer commitOrRollback) {
        this.commitOrRollback = commitOrRollback;
    }

    public Boolean getFromTransactionCheck() {
        return this.fromTransactionCheck;
    }

    public void setFromTransactionCheck(Boolean fromTransactionCheck) {
        this.fromTransactionCheck = fromTransactionCheck;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String toString() {
        return "EndTransactionRequestHeader{producerGroup='" + this.producerGroup + "', tranStateTableOffset=" + this.tranStateTableOffset + ", commitLogOffset=" + this.commitLogOffset + ", commitOrRollback=" + this.commitOrRollback + ", fromTransactionCheck=" + this.fromTransactionCheck + ", msgId='" + this.msgId + "', transactionId='" + this.transactionId + "'}";
    }
}
