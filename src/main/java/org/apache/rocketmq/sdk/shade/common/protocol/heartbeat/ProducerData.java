package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

import org.springframework.beans.PropertyAccessor;

public class ProducerData {
    private String groupName;

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String toString() {
        return "ProducerData [groupName=" + this.groupName + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
