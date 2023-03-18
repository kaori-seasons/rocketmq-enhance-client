package org.apache.rocketmq.sdk.api.exactlyonce;

public class MQUnsupportException extends RuntimeException {
    private int code;

    public MQUnsupportException() {
    }

    public MQUnsupportException(String message) {
        super(message);
    }

    public MQUnsupportException(String message, int code) {
        super(message);
        this.code = code;
    }

    public MQUnsupportException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public MQUnsupportException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
