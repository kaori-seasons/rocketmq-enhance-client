package org.apache.rocketmq.sdk.shade.client.hook;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import java.util.List;

import org.springframework.beans.PropertyAccessor;

public class FilterMessageContext {
    private String consumerGroup;
    private List<MessageExt> msgList;
    private MessageQueue mq;
    private Object arg;
    private boolean unitMode;

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public List<MessageExt> getMsgList() {
        return this.msgList;
    }

    public void setMsgList(List<MessageExt> msgList) {
        this.msgList = msgList;
    }

    public MessageQueue getMq() {
        return this.mq;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public Object getArg() {
        return this.arg;
    }

    public void setArg(Object arg) {
        this.arg = arg;
    }

    public boolean isUnitMode() {
        return this.unitMode;
    }

    public void setUnitMode(boolean isUnitMode) {
        this.unitMode = isUnitMode;
    }

    public String toString() {
        return "ConsumeMessageContext [consumerGroup=" + this.consumerGroup + ", msgList=" + this.msgList + ", mq=" + this.mq + ", arg=" + this.arg + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
