package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashMap;
import java.util.Map;

public class QueryCorrectionOffsetBody extends RemotingSerializable {
    private Map<Integer, Long> correctionOffsets = new HashMap();

    public Map<Integer, Long> getCorrectionOffsets() {
        return this.correctionOffsets;
    }

    public void setCorrectionOffsets(Map<Integer, Long> correctionOffsets) {
        this.correctionOffsets = correctionOffsets;
    }
}
