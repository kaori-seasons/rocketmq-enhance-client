package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class ResetOffsetRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String topic;
    @CFNotNull
    private String group;
    @CFNotNull
    private long timestamp;
    @CFNotNull
    private boolean isForce;

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isForce() {
        return this.isForce;
    }

    public void setForce(boolean isForce) {
        this.isForce = isForce;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
