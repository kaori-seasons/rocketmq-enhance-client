package org.apache.rocketmq.sdk.shade.client.consumer.store;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class OffsetSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<MessageQueue, AtomicLong> offsetTable = new ConcurrentHashMap();

    public ConcurrentMap<MessageQueue, AtomicLong> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(ConcurrentMap<MessageQueue, AtomicLong> offsetTable) {
        this.offsetTable = offsetTable;
    }
}
