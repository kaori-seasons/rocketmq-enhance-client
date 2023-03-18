package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;

public class ProducerConnection extends RemotingSerializable {
    private HashSet<Connection> connectionSet = new HashSet<>();

    public HashSet<Connection> getConnectionSet() {
        return this.connectionSet;
    }

    public void setConnectionSet(HashSet<Connection> connectionSet) {
        this.connectionSet = connectionSet;
    }
}
