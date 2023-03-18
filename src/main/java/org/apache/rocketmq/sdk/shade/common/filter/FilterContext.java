package org.apache.rocketmq.sdk.shade.common.filter;

public class FilterContext {
    private String consumerGroup;

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
