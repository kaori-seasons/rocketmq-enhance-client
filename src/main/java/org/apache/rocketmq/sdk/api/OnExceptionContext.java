package org.apache.rocketmq.sdk.api;

import org.apache.rocketmq.sdk.api.exception.RMQClientException;

public class OnExceptionContext {
    private String messageId;
    private String topic;
    private RMQClientException exception;

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

    public RMQClientException getException() {
        return this.exception;
    }

    public void setException(RMQClientException exception) {
        this.exception = exception;
    }
}
