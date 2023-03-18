package org.apache.rocketmq.sdk.api.exception;

public class RMQClientException extends RuntimeException {
    public RMQClientException() {
    }

    public RMQClientException(String message) {
        super(message);
    }

    public RMQClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RMQClientException(Throwable cause) {
        super(cause);
    }
}
