package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

public class ExecResult<T> {
    private T result;
    private boolean sucess;
    private String message;

    public ExecResult(T result) {
        this.result = result;
    }

    public ExecResult(T result, String message) {
        this.result = result;
        this.message = message;
    }

    public ExecResult(T result, boolean sucess, String message) {
        this.result = result;
        this.sucess = sucess;
        this.message = message;
    }

    public T getResult() {
        return this.result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public boolean isSucess() {
        return this.sucess;
    }

    public void setSucess(boolean sucess) {
        this.sucess = sucess;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
