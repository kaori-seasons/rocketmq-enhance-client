package org.apache.rocketmq.sdk.api.bean;

import org.springframework.beans.PropertyAccessor;

public class Subscription {
    private String topic;
    private String expression;
    private String type;

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getExpression() {
        return this.expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int hashCode() {
        return (31 * 1) + (this.topic == null ? 0 : this.topic.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Subscription other = (Subscription) obj;
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

    public String toString() {
        return "Subscription [topic=" + this.topic + ", expression=" + this.expression + ", type=" + this.type + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
