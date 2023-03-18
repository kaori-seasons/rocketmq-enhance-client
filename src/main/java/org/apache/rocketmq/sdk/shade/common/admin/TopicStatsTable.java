package org.apache.rocketmq.sdk.shade.common.admin;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashMap;

public class TopicStatsTable extends RemotingSerializable {
    private HashMap<MessageQueue, TopicOffset> offsetTable = new HashMap<>();

    public HashMap<MessageQueue, TopicOffset> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(HashMap<MessageQueue, TopicOffset> offsetTable) {
        this.offsetTable = offsetTable;
    }
}
