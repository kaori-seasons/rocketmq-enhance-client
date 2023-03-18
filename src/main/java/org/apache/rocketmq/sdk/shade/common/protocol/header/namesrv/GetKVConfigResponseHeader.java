package org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class GetKVConfigResponseHeader implements CommandCustomHeader {
    @CFNullable
    private String value;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
