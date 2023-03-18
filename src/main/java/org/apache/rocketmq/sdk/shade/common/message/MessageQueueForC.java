package org.apache.rocketmq.sdk.shade.common.message;

import java.io.Serializable;
import org.springframework.beans.PropertyAccessor;

public class MessageQueueForC implements Comparable<MessageQueueForC>, Serializable {
    private static final long serialVersionUID = 5320967846569962104L;
    private String topic;
    private String brokerName;
    private int queueId;
    private long offset;

    public MessageQueueForC(String topic, String brokerName, int queueId, long offset) {
        this.topic = topic;
        this.brokerName = brokerName;
        this.queueId = queueId;
        this.offset = offset;
    }

    public int compareTo(MessageQueueForC o) {
        int result = this.topic.compareTo(o.topic);
        if (result != 0) {
            return result;
        }
        int result2 = this.brokerName.compareTo(o.brokerName);
        if (result2 != 0) {
            return result2;
        }
        int result3 = this.queueId - o.queueId;
        if (result3 != 0) {
            return result3;
        }
        if (this.offset - o.offset > 0) {
            return 1;
        }
        if (this.offset - o.offset == 0) {
            return 0;
        }
        return -1;
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
        MessageQueueForC other = (MessageQueueForC) obj;
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
        } else if (!this.topic.equals(other.topic)) {
            return false;
        }
        if (this.offset != other.offset) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MessageQueueForC [topic=" + this.topic + ", brokerName=" + this.brokerName + ", queueId=" + this.queueId + ", offset=" + this.offset + PropertyAccessor.PROPERTY_KEY_SUFFIX;
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

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
