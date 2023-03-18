package org.apache.rocketmq.sdk.trace.core.dispatch;

import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import java.io.IOException;

public interface AsyncDispatcher {
    void start() throws MQClientException;

    boolean append(Object obj);

    void flush() throws IOException;

    void shutdown();
}
