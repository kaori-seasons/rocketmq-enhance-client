package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class QueryMessageResponseHeader implements CommandCustomHeader {
    @CFNotNull
    private Long indexLastUpdateTimestamp;
    @CFNotNull
    private Long indexLastUpdatePhyoffset;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public Long getIndexLastUpdateTimestamp() {
        return this.indexLastUpdateTimestamp;
    }

    public void setIndexLastUpdateTimestamp(Long indexLastUpdateTimestamp) {
        this.indexLastUpdateTimestamp = indexLastUpdateTimestamp;
    }

    public Long getIndexLastUpdatePhyoffset() {
        return this.indexLastUpdatePhyoffset;
    }

    public void setIndexLastUpdatePhyoffset(Long indexLastUpdatePhyoffset) {
        this.indexLastUpdatePhyoffset = indexLastUpdatePhyoffset;
    }
}
