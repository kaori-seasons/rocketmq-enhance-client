package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.PropertyAccessor;

public class HeartbeatData extends RemotingSerializable {
    private String clientID;
    private Set<ProducerData> producerDataSet = new HashSet();
    private Set<ConsumerData> consumerDataSet = new HashSet();

    public String getClientID() {
        return this.clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public Set<ProducerData> getProducerDataSet() {
        return this.producerDataSet;
    }

    public void setProducerDataSet(Set<ProducerData> producerDataSet) {
        this.producerDataSet = producerDataSet;
    }

    public Set<ConsumerData> getConsumerDataSet() {
        return this.consumerDataSet;
    }

    public void setConsumerDataSet(Set<ConsumerData> consumerDataSet) {
        this.consumerDataSet = consumerDataSet;
    }

    public String toString() {
        return "HeartbeatData [clientID=" + this.clientID + ", producerDataSet=" + this.producerDataSet + ", consumerDataSet=" + this.consumerDataSet + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
