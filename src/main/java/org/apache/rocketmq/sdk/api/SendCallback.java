package org.apache.rocketmq.sdk.api;

public interface SendCallback {
    void onSuccess(SendResult sendResult);

    void onException(OnExceptionContext onExceptionContext);
}
