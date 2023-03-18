package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class CheckTransactionStateRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private Long tranStateTableOffset;
    @CFNotNull
    private Long commitLogOffset;
    private String msgId;
    private String transactionId;
    private String offsetMsgId;

    @Override
    public void checkFields() throws RemotingCommandException {
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

    public String getOffsetMsgId() {
        return this.offsetMsgId;
    }

    public void setOffsetMsgId(String offsetMsgId) {
        this.offsetMsgId = offsetMsgId;
    }
}
