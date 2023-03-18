package org.apache.rocketmq.sdk.shade.common.namesrv;

import org.apache.rocketmq.sdk.shade.common.protocol.body.KVTable;

public class RegisterBrokerResult {
    private String haServerAddr;
    private String masterAddr;
    private KVTable kvTable;

    public String getHaServerAddr() {
        return this.haServerAddr;
    }

    public void setHaServerAddr(String haServerAddr) {
        this.haServerAddr = haServerAddr;
    }

    public String getMasterAddr() {
        return this.masterAddr;
    }

    public void setMasterAddr(String masterAddr) {
        this.masterAddr = masterAddr;
    }

    public KVTable getKvTable() {
        return this.kvTable;
    }

    public void setKvTable(KVTable kvTable) {
        this.kvTable = kvTable;
    }
}
