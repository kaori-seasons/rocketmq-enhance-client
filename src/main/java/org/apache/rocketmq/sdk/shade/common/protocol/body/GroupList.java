package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;

public class GroupList extends RemotingSerializable {
    private HashSet<String> groupList = new HashSet<>();

    public HashSet<String> getGroupList() {
        return this.groupList;
    }

    public void setGroupList(HashSet<String> groupList) {
        this.groupList = groupList;
    }
}
