package org.apache.rocketmq.sdk.shade.client.consumer;

public interface PullCallback {
    void onSuccess(PullResult pullResult);

    void onException(Throwable th);
}
