package org.apache.rocketmq.sdk.shade.common.acl;

public class AclException extends RuntimeException {
    private static final long serialVersionUID = -7256002576788700354L;
    private String status;
    private int code;

    public AclException(String status, int code) {
        this.status = status;
        this.code = code;
    }

    public AclException(String status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public AclException(String message) {
        super(message);
    }

    public AclException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public AclException(String status, int code, String message, Throwable throwable) {
        super(message, throwable);
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
