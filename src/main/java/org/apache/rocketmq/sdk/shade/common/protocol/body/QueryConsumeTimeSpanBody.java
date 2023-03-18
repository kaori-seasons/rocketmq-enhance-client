package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.ArrayList;
import java.util.List;

public class QueryConsumeTimeSpanBody extends RemotingSerializable {
    List<QueueTimeSpan> consumeTimeSpanSet = new ArrayList();

    public List<QueueTimeSpan> getConsumeTimeSpanSet() {
        return this.consumeTimeSpanSet;
    }

    public void setConsumeTimeSpanSet(List<QueueTimeSpan> consumeTimeSpanSet) {
        this.consumeTimeSpanSet = consumeTimeSpanSet;
    }
}
