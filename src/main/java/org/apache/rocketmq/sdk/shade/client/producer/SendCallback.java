package org.apache.rocketmq.sdk.shade.client.producer;

public interface SendCallback {
    void onSuccess(SendResult sendResult);

    void onException(Throwable th);
}
