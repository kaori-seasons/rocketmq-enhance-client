package org.apache.rocketmq.sdk.api.order;

import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.admin.Admin;

public interface OrderConsumer extends Admin {
    @Override
    void start();

    @Override
    void shutdown();

    void subscribe(String str, String str2, MessageOrderListener messageOrderListener);

    void subscribe(String str, MessageSelector messageSelector, MessageOrderListener messageOrderListener);
}
