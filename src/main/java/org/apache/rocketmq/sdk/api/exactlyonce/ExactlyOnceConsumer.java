package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.admin.Admin;

public interface ExactlyOnceConsumer extends Admin {
    @Override 
    void start();

    @Override 
    void shutdown();

    void subscribe(String str, String str2, MessageListener messageListener);

    void subscribe(String str, MessageSelector messageSelector, MessageListener messageListener);
}
