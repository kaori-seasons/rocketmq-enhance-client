package org.apache.rocketmq.sdk.api;

public class SendResult {
    private String messageId;
    private String topic;

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String toString() {
        return "SendResult[topic=" + this.topic + ", messageId=" + this.messageId + ']';
    }
}
