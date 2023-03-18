package org.apache.rocketmq.sdk.shade.common;

import java.util.concurrent.atomic.AtomicBoolean;

public class BrokerConfigSingleton {
    private static AtomicBoolean isInit = new AtomicBoolean();
    private static BrokerConfig brokerConfig;

    public static BrokerConfig getBrokerConfig() {
        if (brokerConfig != null) {
            return brokerConfig;
        }
        throw new IllegalArgumentException("brokerConfig Cannot be null !");
    }

    public static void setBrokerConfig(BrokerConfig brokerConfig2) {
        if (!isInit.compareAndSet(false, true)) {
            throw new IllegalArgumentException("broker config have inited !");
        }
        brokerConfig = brokerConfig2;
    }
}
