package org.apache.rocketmq.sdk.api.admin;

import java.util.Properties;

public interface Admin {
    boolean isStarted();

    boolean isClosed();

    void start();

    void updateCredential(Properties properties);

    void shutdown();
}
