package org.apache.rocketmq.sdk.shade.common.protocol.header.filtersrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class RegisterFilterServerRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String filterServerAddr;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getFilterServerAddr() {
        return this.filterServerAddr;
    }

    public void setFilterServerAddr(String filterServerAddr) {
        this.filterServerAddr = filterServerAddr;
    }
}
