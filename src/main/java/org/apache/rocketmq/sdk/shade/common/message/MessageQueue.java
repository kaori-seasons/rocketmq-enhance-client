package org.apache.rocketmq.sdk.shade.common.message;

import java.io.Serializable;
import org.springframework.beans.PropertyAccessor;

public class MessageQueue implements Comparable<MessageQueue>, Serializable {
    private static final long serialVersionUID = 6191200464116433425L;
    private String topic;
    private String brokerName;
    private int queueId;

    public MessageQueue() {
    }

    public MessageQueue(String topic, String brokerName, int queueId) {
        this.topic = topic;
        this.brokerName = brokerName;
        this.queueId = queueId;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return this.queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * ((31 * 1) + (this.brokerName == null ? 0 : this.brokerName.hashCode()))) + this.queueId)) + (this.topic == null ? 0 : this.topic.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageQueue other = (MessageQueue) obj;
        if (this.brokerName == null) {
            if (other.brokerName != null) {
                return false;
            }
        } else if (!this.brokerName.equals(other.brokerName)) {
            return false;
        }
        if (this.queueId != other.queueId) {
            return false;
        }
        if (this.topic == null) {
            if (other.topic != null) {
                return false;
            }
            return true;
        } else if (!this.topic.equals(other.topic)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "MessageQueue [topic=" + this.topic + ", brokerName=" + this.brokerName + ", queueId=" + this.queueId + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public int compareTo(MessageQueue o) {
        int result = this.topic.compareTo(o.topic);
        if (result != 0) {
            return result;
        }
        int result2 = this.brokerName.compareTo(o.brokerName);
        if (result2 != 0) {
            return result2;
        }
        return this.queueId - o.queueId;
    }
}
