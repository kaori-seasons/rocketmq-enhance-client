package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashMap;

public class KVTable extends RemotingSerializable {
    private HashMap<String, String> table = new HashMap<>();

    public HashMap<String, String> getTable() {
        return this.table;
    }

    public void setTable(HashMap<String, String> table) {
        this.table = table;
    }
}
