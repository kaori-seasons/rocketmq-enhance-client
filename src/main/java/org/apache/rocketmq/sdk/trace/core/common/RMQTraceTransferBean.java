package org.apache.rocketmq.sdk.trace.core.common;

import java.util.HashSet;
import java.util.Set;

public class RMQTraceTransferBean {
    private String transData;
    private Set<String> transKey = new HashSet();

    public String getTransData() {
        return this.transData;
    }

    public void setTransData(String transData) {
        this.transData = transData;
    }

    public Set<String> getTransKey() {
        return this.transKey;
    }

    public void setTransKey(Set<String> transKey) {
        this.transKey = transKey;
    }
}
