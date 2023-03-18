package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

public enum ConsumeType {
    CONSUME_ACTIVELY("PULL"),
    CONSUME_PASSIVELY("PUSH");
    
    private String typeCN;

    ConsumeType(String typeCN) {
        this.typeCN = typeCN;
    }

    public String getTypeCN() {
        return this.typeCN;
    }
}
