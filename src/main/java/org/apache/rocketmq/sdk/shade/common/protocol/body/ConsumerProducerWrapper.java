package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;
import java.util.Set;

public class ConsumerProducerWrapper extends RemotingSerializable {
    private Set<String> consumerList = new HashSet();
    private String producerId;

    public Set<String> getConsumerList() {
        return this.consumerList;
    }

    public void setConsumerList(Set<String> consumerList) {
        this.consumerList = consumerList;
    }

    public String getProducerId() {
        return this.producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }
}
