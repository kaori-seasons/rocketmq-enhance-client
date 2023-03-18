package org.apache.rocketmq.sdk.shade.common.subscription;

import org.springframework.beans.PropertyAccessor;

public class SubscriptionGroupConfig {
    private String groupName;
    private boolean consumeEnable = true;
    private boolean consumeFromMinEnable = true;
    private boolean consumeBroadcastEnable = true;
    private int retryQueueNums = 1;
    private int retryMaxTimes = 16;
    private long brokerId = 0;
    private long whichBrokerWhenConsumeSlowly = 1;
    private boolean notifyConsumerIdsChangedEnable = true;

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isConsumeEnable() {
        return this.consumeEnable;
    }

    public void setConsumeEnable(boolean consumeEnable) {
        this.consumeEnable = consumeEnable;
    }

    public boolean isConsumeFromMinEnable() {
        return this.consumeFromMinEnable;
    }

    public void setConsumeFromMinEnable(boolean consumeFromMinEnable) {
        this.consumeFromMinEnable = consumeFromMinEnable;
    }

    public boolean isConsumeBroadcastEnable() {
        return this.consumeBroadcastEnable;
    }

    public void setConsumeBroadcastEnable(boolean consumeBroadcastEnable) {
        this.consumeBroadcastEnable = consumeBroadcastEnable;
    }

    public int getRetryQueueNums() {
        return this.retryQueueNums;
    }

    public void setRetryQueueNums(int retryQueueNums) {
        this.retryQueueNums = retryQueueNums;
    }

    public int getRetryMaxTimes() {
        return this.retryMaxTimes;
    }

    public void setRetryMaxTimes(int retryMaxTimes) {
        this.retryMaxTimes = retryMaxTimes;
    }

    public long getBrokerId() {
        return this.brokerId;
    }

    public void setBrokerId(long brokerId) {
        this.brokerId = brokerId;
    }

    public long getWhichBrokerWhenConsumeSlowly() {
        return this.whichBrokerWhenConsumeSlowly;
    }

    public void setWhichBrokerWhenConsumeSlowly(long whichBrokerWhenConsumeSlowly) {
        this.whichBrokerWhenConsumeSlowly = whichBrokerWhenConsumeSlowly;
    }

    public boolean isNotifyConsumerIdsChangedEnable() {
        return this.notifyConsumerIdsChangedEnable;
    }

    public void setNotifyConsumerIdsChangedEnable(boolean notifyConsumerIdsChangedEnable) {
        this.notifyConsumerIdsChangedEnable = notifyConsumerIdsChangedEnable;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * 1) + ((int) (this.brokerId ^ (this.brokerId >>> 32))))) + (this.consumeBroadcastEnable ? 1231 : 1237))) + (this.consumeEnable ? 1231 : 1237))) + (this.consumeFromMinEnable ? 1231 : 1237))) + (this.notifyConsumerIdsChangedEnable ? 1231 : 1237))) + (this.groupName == null ? 0 : this.groupName.hashCode()))) + this.retryMaxTimes)) + this.retryQueueNums)) + ((int) (this.whichBrokerWhenConsumeSlowly ^ (this.whichBrokerWhenConsumeSlowly >>> 32)));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SubscriptionGroupConfig other = (SubscriptionGroupConfig) obj;
        if (this.brokerId != other.brokerId || this.consumeBroadcastEnable != other.consumeBroadcastEnable || this.consumeEnable != other.consumeEnable || this.consumeFromMinEnable != other.consumeFromMinEnable) {
            return false;
        }
        if (this.groupName == null) {
            if (other.groupName != null) {
                return false;
            }
        } else if (!this.groupName.equals(other.groupName)) {
            return false;
        }
        if (this.retryMaxTimes == other.retryMaxTimes && this.retryQueueNums == other.retryQueueNums && this.whichBrokerWhenConsumeSlowly == other.whichBrokerWhenConsumeSlowly && this.notifyConsumerIdsChangedEnable == other.notifyConsumerIdsChangedEnable) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "SubscriptionGroupConfig [groupName=" + this.groupName + ", consumeEnable=" + this.consumeEnable + ", consumeFromMinEnable=" + this.consumeFromMinEnable + ", consumeBroadcastEnable=" + this.consumeBroadcastEnable + ", retryQueueNums=" + this.retryQueueNums + ", retryMaxTimes=" + this.retryMaxTimes + ", brokerId=" + this.brokerId + ", whichBrokerWhenConsumeSlowly=" + this.whichBrokerWhenConsumeSlowly + ", notifyConsumerIdsChangedEnable=" + this.notifyConsumerIdsChangedEnable + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
