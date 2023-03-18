package org.apache.rocketmq.sdk.api.batch;

import org.apache.rocketmq.sdk.api.admin.Admin;

public interface BatchConsumer extends Admin {
    void subscribe(String str, String str2, BatchMessageListener batchMessageListener);

    void unsubscribe(String str);
}
