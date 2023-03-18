package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.Map;

public class ResetOffsetBody extends RemotingSerializable {
    private Map<MessageQueue, Long> offsetTable;

    public Map<MessageQueue, Long> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(Map<MessageQueue, Long> offsetTable) {
        this.offsetTable = offsetTable;
    }
}
