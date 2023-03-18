package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.asm.Opcodes;

public class ConsumerOffsetSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable = new ConcurrentHashMap((int) Opcodes.ACC_INTERFACE);

    public ConcurrentMap<String, ConcurrentMap<Integer, Long>> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable) {
        this.offsetTable = offsetTable;
    }
}
