package org.apache.rocketmq.sdk.shade.client.consumer.listener;

import org.apache.rocketmq.sdk.shade.client.hook.CheckSendBackHook;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public abstract class ConsumeConcurrentlyContext {
    private CheckSendBackHook checkSendBackHook;
    private int delayLevelWhenNextConsume = 0;
    protected int ackIndex = Integer.MAX_VALUE;
    private ConsumeExactlyOnceStatus exactlyOnceStatus = ConsumeExactlyOnceStatus.NO_EXACTLYONCE;

    public abstract MessageQueue getMessageQueue();

    public int getDelayLevelWhenNextConsume() {
        return this.delayLevelWhenNextConsume;
    }

    public void setDelayLevelWhenNextConsume(int delayLevelWhenNextConsume) {
        this.delayLevelWhenNextConsume = delayLevelWhenNextConsume;
    }

    public int getAckIndex() {
        return this.ackIndex;
    }

    public void setAckIndex(int ackIndex) {
        this.ackIndex = ackIndex;
    }

    public CheckSendBackHook getCheckSendBackHook() {
        return this.checkSendBackHook;
    }

    public void setCheckSendBackHook(CheckSendBackHook checkSendBackHook) {
        this.checkSendBackHook = checkSendBackHook;
    }

    public ConsumeExactlyOnceStatus getExactlyOnceStatus() {
        return this.exactlyOnceStatus;
    }

    public void setExactlyOnceStatus(ConsumeExactlyOnceStatus exactlyOnceStatus) {
        this.exactlyOnceStatus = exactlyOnceStatus;
    }
}
