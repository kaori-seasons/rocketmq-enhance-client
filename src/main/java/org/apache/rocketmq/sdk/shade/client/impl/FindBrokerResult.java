package org.apache.rocketmq.sdk.shade.client.impl;

public class FindBrokerResult {
    private final String brokerAddr;
    private final boolean slave;
    private final int brokerVersion;

    public FindBrokerResult(String brokerAddr, boolean slave) {
        this.brokerAddr = brokerAddr;
        this.slave = slave;
        this.brokerVersion = 0;
    }

    public FindBrokerResult(String brokerAddr, boolean slave, int brokerVersion) {
        this.brokerAddr = brokerAddr;
        this.slave = slave;
        this.brokerVersion = brokerVersion;
    }

    public String getBrokerAddr() {
        return this.brokerAddr;
    }

    public boolean isSlave() {
        return this.slave;
    }

    public int getBrokerVersion() {
        return this.brokerVersion;
    }
}
