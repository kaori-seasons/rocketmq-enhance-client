package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

import org.apache.rocketmq.sdk.api.PropertyValueConst;

public enum MessageModel {
    BROADCASTING(PropertyValueConst.BROADCASTING),
    CLUSTERING(PropertyValueConst.CLUSTERING);
    
    private String modeCN;

    MessageModel(String modeCN) {
        this.modeCN = modeCN;
    }

    public String getModeCN() {
        return this.modeCN;
    }
}
