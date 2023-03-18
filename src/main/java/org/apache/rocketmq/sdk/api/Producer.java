package org.apache.rocketmq.sdk.api;

import org.apache.rocketmq.sdk.api.admin.Admin;

import java.util.concurrent.ExecutorService;

public interface Producer extends Admin {
    @Override
    void start();

    @Override
    void shutdown();

    SendResult send(Message message);

    void sendOneway(Message message);

    void sendAsync(Message message, SendCallback sendCallback);

    void setCallbackExecutor(ExecutorService executorService);
}
