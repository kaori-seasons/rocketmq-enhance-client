package org.apache.rocketmq.sdk.shade.common.protocol.header.iam;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class DeleteUserRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String userId;

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
