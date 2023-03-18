package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class ViewBrokerStatsDataRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String statsName;
    @CFNotNull
    private String statsKey;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getStatsName() {
        return this.statsName;
    }

    public void setStatsName(String statsName) {
        this.statsName = statsName;
    }

    public String getStatsKey() {
        return this.statsKey;
    }

    public void setStatsKey(String statsKey) {
        this.statsKey = statsKey;
    }
}
