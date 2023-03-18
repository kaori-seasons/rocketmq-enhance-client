package org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class QueryDataVersionResponseHeader implements CommandCustomHeader {
    @CFNotNull
    private Boolean changed;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public Boolean getChanged() {
        return this.changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("QueryDataVersionResponseHeader{");
        sb.append("changed=").append(this.changed);
        sb.append('}');
        return sb.toString();
    }
}
