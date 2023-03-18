package org.apache.rocketmq.sdk.shade.common.protocol.header.iam;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class RegisterOrUnRegisterTopicConsumerRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String topic;
    @CFNotNull
    private String consumerName;

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerName() {
        return this.consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
