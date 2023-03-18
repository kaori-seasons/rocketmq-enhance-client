package org.apache.rocketmq.sdk.shade.common.protocol.route;

import org.springframework.beans.PropertyAccessor;

public class QueueData implements Comparable<QueueData> {
    private String brokerName;
    private int readQueueNums;
    private int writeQueueNums;
    private int perm;
    private int topicSynFlag;

    public int getReadQueueNums() {
        return this.readQueueNums;
    }

    public void setReadQueueNums(int readQueueNums) {
        this.readQueueNums = readQueueNums;
    }

    public int getWriteQueueNums() {
        return this.writeQueueNums;
    }

    public void setWriteQueueNums(int writeQueueNums) {
        this.writeQueueNums = writeQueueNums;
    }

    public int getPerm() {
        return this.perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
    }

    public int getTopicSynFlag() {
        return this.topicSynFlag;
    }

    public void setTopicSynFlag(int topicSynFlag) {
        this.topicSynFlag = topicSynFlag;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * 1) + (this.brokerName == null ? 0 : this.brokerName.hashCode()))) + this.perm)) + this.readQueueNums)) + this.writeQueueNums)) + this.topicSynFlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        QueueData other = (QueueData) obj;
        if (this.brokerName == null) {
            if (other.brokerName != null) {
                return false;
            }
        } else if (!this.brokerName.equals(other.brokerName)) {
            return false;
        }
        if (this.perm == other.perm && this.readQueueNums == other.readQueueNums && this.writeQueueNums == other.writeQueueNums && this.topicSynFlag == other.topicSynFlag) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "QueueData [brokerName=" + this.brokerName + ", readQueueNums=" + this.readQueueNums + ", writeQueueNums=" + this.writeQueueNums + ", perm=" + this.perm + ", topicSynFlag=" + this.topicSynFlag + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public int compareTo(QueueData o) {
        return this.brokerName.compareTo(o.getBrokerName());
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }
}
