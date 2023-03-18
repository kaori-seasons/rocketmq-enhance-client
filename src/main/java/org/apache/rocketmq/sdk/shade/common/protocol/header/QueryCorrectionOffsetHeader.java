package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class QueryCorrectionOffsetHeader implements CommandCustomHeader {
    private String filterGroups;
    @CFNotNull
    private String compareGroup;
    @CFNotNull
    private String topic;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getFilterGroups() {
        return this.filterGroups;
    }

    public void setFilterGroups(String filterGroups) {
        this.filterGroups = filterGroups;
    }

    public String getCompareGroup() {
        return this.compareGroup;
    }

    public void setCompareGroup(String compareGroup) {
        this.compareGroup = compareGroup;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
