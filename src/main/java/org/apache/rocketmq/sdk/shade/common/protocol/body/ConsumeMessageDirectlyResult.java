package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import org.springframework.beans.PropertyAccessor;

public class ConsumeMessageDirectlyResult extends RemotingSerializable {
    private boolean order = false;
    private boolean autoCommit = true;
    private CMResult consumeResult;
    private String remark;
    private long spentTimeMills;

    public boolean isOrder() {
        return this.order;
    }

    public void setOrder(boolean order) {
        this.order = order;
    }

    public boolean isAutoCommit() {
        return this.autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public CMResult getConsumeResult() {
        return this.consumeResult;
    }

    public void setConsumeResult(CMResult consumeResult) {
        this.consumeResult = consumeResult;
    }

    public long getSpentTimeMills() {
        return this.spentTimeMills;
    }

    public void setSpentTimeMills(long spentTimeMills) {
        this.spentTimeMills = spentTimeMills;
    }

    public String toString() {
        return "ConsumeMessageDirectlyResult [order=" + this.order + ", autoCommit=" + this.autoCommit + ", consumeResult=" + this.consumeResult + ", remark=" + this.remark + ", spentTimeMills=" + this.spentTimeMills + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
