package org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class RegisterOrderTopicRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String topic;
    @CFNotNull
    private String orderTopicString;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getOrderTopicString() {
        return this.orderTopicString;
    }

    public void setOrderTopicString(String orderTopicString) {
        this.orderTopicString = orderTopicString;
    }
}
