package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import java.util.Date;

public class QueueTimeSpan {
    private MessageQueue messageQueue;
    private long minTimeStamp;
    private long maxTimeStamp;
    private long consumeTimeStamp;
    private long delayTime;

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public long getMinTimeStamp() {
        return this.minTimeStamp;
    }

    public void setMinTimeStamp(long minTimeStamp) {
        this.minTimeStamp = minTimeStamp;
    }

    public long getMaxTimeStamp() {
        return this.maxTimeStamp;
    }

    public void setMaxTimeStamp(long maxTimeStamp) {
        this.maxTimeStamp = maxTimeStamp;
    }

    public long getConsumeTimeStamp() {
        return this.consumeTimeStamp;
    }

    public void setConsumeTimeStamp(long consumeTimeStamp) {
        this.consumeTimeStamp = consumeTimeStamp;
    }

    public String getMinTimeStampStr() {
        return UtilAll.formatDate(new Date(this.minTimeStamp), UtilAll.YYYY_MM_DD_HH_MM_SS_SSS);
    }

    public String getMaxTimeStampStr() {
        return UtilAll.formatDate(new Date(this.maxTimeStamp), UtilAll.YYYY_MM_DD_HH_MM_SS_SSS);
    }

    public String getConsumeTimeStampStr() {
        return UtilAll.formatDate(new Date(this.consumeTimeStamp), UtilAll.YYYY_MM_DD_HH_MM_SS_SSS);
    }

    public long getDelayTime() {
        return this.delayTime;
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }
}
