package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class ViewMessageRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private Long offset;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public Long getOffset() {
        return this.offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }
}
