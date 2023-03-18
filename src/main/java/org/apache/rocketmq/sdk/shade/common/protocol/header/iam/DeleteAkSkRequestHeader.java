package org.apache.rocketmq.sdk.shade.common.protocol.header.iam;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class DeleteAkSkRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String userId;
    @CFNotNull
    private String accessKey;

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
