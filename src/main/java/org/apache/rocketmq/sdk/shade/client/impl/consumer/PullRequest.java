package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.springframework.beans.PropertyAccessor;

public class PullRequest {
    private String consumerGroup;
    private MessageQueue messageQueue;
    private ProcessQueue processQueue;
    private long nextOffset;
    private boolean lockedFirst = false;

    public boolean isLockedFirst() {
        return this.lockedFirst;
    }

    public void setLockedFirst(boolean lockedFirst) {
        this.lockedFirst = lockedFirst;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public long getNextOffset() {
        return this.nextOffset;
    }

    public void setNextOffset(long nextOffset) {
        this.nextOffset = nextOffset;
    }

    public int hashCode() {
        return (31 * ((31 * 1) + (this.consumerGroup == null ? 0 : this.consumerGroup.hashCode()))) + (this.messageQueue == null ? 0 : this.messageQueue.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PullRequest other = (PullRequest) obj;
        if (this.consumerGroup == null) {
            if (other.consumerGroup != null) {
                return false;
            }
        } else if (!this.consumerGroup.equals(other.consumerGroup)) {
            return false;
        }
        if (this.messageQueue == null) {
            if (other.messageQueue != null) {
                return false;
            }
            return true;
        } else if (!this.messageQueue.equals(other.messageQueue)) {
            return false;
        } else {
            return true;
        }
    }

    public String toString() {
        return "PullRequest [consumerGroup=" + this.consumerGroup + ", messageQueue=" + this.messageQueue + ", nextOffset=" + this.nextOffset + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public ProcessQueue getProcessQueue() {
        return this.processQueue;
    }

    public void setProcessQueue(ProcessQueue processQueue) {
        this.processQueue = processQueue;
    }
}
