package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.DataVersion;
import org.apache.rocketmq.sdk.shade.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SubscriptionGroupWrapper extends RemotingSerializable {
    private ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable = new ConcurrentHashMap(1024);
    private DataVersion dataVersion = new DataVersion();

    public ConcurrentMap<String, SubscriptionGroupConfig> getSubscriptionGroupTable() {
        return this.subscriptionGroupTable;
    }

    public void setSubscriptionGroupTable(ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable) {
        this.subscriptionGroupTable = subscriptionGroupTable;
    }

    public DataVersion getDataVersion() {
        return this.dataVersion;
    }

    public void setDataVersion(DataVersion dataVersion) {
        this.dataVersion = dataVersion;
    }
}
