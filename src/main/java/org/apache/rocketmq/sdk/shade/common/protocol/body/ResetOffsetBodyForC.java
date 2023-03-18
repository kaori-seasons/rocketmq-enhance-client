package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueueForC;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.List;

public class ResetOffsetBodyForC extends RemotingSerializable {
    private List<MessageQueueForC> offsetTable;

    public List<MessageQueueForC> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(List<MessageQueueForC> offsetTable) {
        this.offsetTable = offsetTable;
    }
}
