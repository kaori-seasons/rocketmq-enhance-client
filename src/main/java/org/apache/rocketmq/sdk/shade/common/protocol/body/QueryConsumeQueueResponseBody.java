package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.List;

public class QueryConsumeQueueResponseBody extends RemotingSerializable {
    private SubscriptionData subscriptionData;
    private String filterData;
    private List<ConsumeQueueData> queueData;
    private long maxQueueIndex;
    private long minQueueIndex;

    public SubscriptionData getSubscriptionData() {
        return this.subscriptionData;
    }

    public void setSubscriptionData(SubscriptionData subscriptionData) {
        this.subscriptionData = subscriptionData;
    }

    public String getFilterData() {
        return this.filterData;
    }

    public void setFilterData(String filterData) {
        this.filterData = filterData;
    }

    public List<ConsumeQueueData> getQueueData() {
        return this.queueData;
    }

    public void setQueueData(List<ConsumeQueueData> queueData) {
        this.queueData = queueData;
    }

    public long getMaxQueueIndex() {
        return this.maxQueueIndex;
    }

    public void setMaxQueueIndex(long maxQueueIndex) {
        this.maxQueueIndex = maxQueueIndex;
    }

    public long getMinQueueIndex() {
        return this.minQueueIndex;
    }

    public void setMinQueueIndex(long minQueueIndex) {
        this.minQueueIndex = minQueueIndex;
    }
}
