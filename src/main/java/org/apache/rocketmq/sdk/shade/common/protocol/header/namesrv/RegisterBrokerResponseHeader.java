package org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class RegisterBrokerResponseHeader implements CommandCustomHeader {
    @CFNullable
    private String haServerAddr;
    @CFNullable
    private String masterAddr;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

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
}
