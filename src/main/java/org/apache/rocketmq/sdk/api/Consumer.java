package org.apache.rocketmq.sdk.api;

import org.apache.rocketmq.sdk.api.admin.Admin;

public interface Consumer extends Admin {
    void subscribe(String str, String str2, MessageListener messageListener);

    void subscribe(String str, MessageSelector messageSelector, MessageListener messageListener);

    void unsubscribe(String str);
}
