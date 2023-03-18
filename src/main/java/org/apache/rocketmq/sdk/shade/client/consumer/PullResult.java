package org.apache.rocketmq.sdk.shade.client.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import java.util.List;

import org.springframework.beans.PropertyAccessor;

public class PullResult {
    private final PullStatus pullStatus;
    private final long nextBeginOffset;
    private final long minOffset;
    private final long maxOffset;
    private List<MessageExt> msgFoundList;

    public PullResult(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset, List<MessageExt> msgFoundList) {
        this.pullStatus = pullStatus;
        this.nextBeginOffset = nextBeginOffset;
        this.minOffset = minOffset;
        this.maxOffset = maxOffset;
        this.msgFoundList = msgFoundList;
    }

    public PullStatus getPullStatus() {
        return this.pullStatus;
    }

    public long getNextBeginOffset() {
        return this.nextBeginOffset;
    }

    public long getMinOffset() {
        return this.minOffset;
    }

    public long getMaxOffset() {
        return this.maxOffset;
    }

    public List<MessageExt> getMsgFoundList() {
        return this.msgFoundList;
    }

    public void setMsgFoundList(List<MessageExt> msgFoundList) {
        this.msgFoundList = msgFoundList;
    }

    public String toString() {
        return "PullResult [pullStatus=" + this.pullStatus + ", nextBeginOffset=" + this.nextBeginOffset + ", minOffset=" + this.minOffset + ", maxOffset=" + this.maxOffset + ", msgFoundList=" + (this.msgFoundList == null ? 0 : this.msgFoundList.size()) + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
