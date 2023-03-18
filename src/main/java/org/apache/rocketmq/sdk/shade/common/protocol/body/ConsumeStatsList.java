package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.admin.ConsumeStats;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsumeStatsList extends RemotingSerializable {
    private List<Map<String, List<ConsumeStats>>> consumeStatsList = new ArrayList();
    private String brokerAddr;
    private long totalDiff;

    public List<Map<String, List<ConsumeStats>>> getConsumeStatsList() {
        return this.consumeStatsList;
    }

    public void setConsumeStatsList(List<Map<String, List<ConsumeStats>>> consumeStatsList) {
        this.consumeStatsList = consumeStatsList;
    }

    public String getBrokerAddr() {
        return this.brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public long getTotalDiff() {
        return this.totalDiff;
    }

    public void setTotalDiff(long totalDiff) {
        this.totalDiff = totalDiff;
    }
}
