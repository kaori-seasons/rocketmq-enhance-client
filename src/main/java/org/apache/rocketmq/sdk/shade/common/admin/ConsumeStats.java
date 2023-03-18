package org.apache.rocketmq.sdk.shade.common.admin;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashMap;
import java.util.Map;

public class ConsumeStats extends RemotingSerializable {
    private HashMap<MessageQueue, OffsetWrapper> offsetTable = new HashMap<>();
    private double consumeTps = 0.0d;

    public long computeTotalDiff() {
        char c = 0;
        for (Map.Entry<MessageQueue, OffsetWrapper> next : this.offsetTable.entrySet()) {
            c += next.getValue().getBrokerOffset() - next.getValue().getConsumerOffset();
        }
        return c;
    }

    public HashMap<MessageQueue, OffsetWrapper> getOffsetTable() {
        return this.offsetTable;
    }

    public void setOffsetTable(HashMap<MessageQueue, OffsetWrapper> offsetTable) {
        this.offsetTable = offsetTable;
    }

    public double getConsumeTps() {
        return this.consumeTps;
    }

    public void setConsumeTps(double consumeTps) {
        this.consumeTps = consumeTps;
    }
}
