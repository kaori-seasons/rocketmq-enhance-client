package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.List;

public class GetConsumerListByGroupResponseBody extends RemotingSerializable {
    private List<String> consumerIdList;

    public List<String> getConsumerIdList() {
        return this.consumerIdList;
    }

    public void setConsumerIdList(List<String> consumerIdList) {
        this.consumerIdList = consumerIdList;
    }
}
