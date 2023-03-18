package org.apache.rocketmq.sdk.shade.common.iam;

import java.util.List;

public class FetchAuthInfoResult {
    private String sk_perm;
    private String producerId;
    private List<String> consumerIds;
    private int resultCode;

    public String getSk_perm() {
        return this.sk_perm;
    }

    public void setSk_perm(String sk_perm) {
        this.sk_perm = sk_perm;
    }

    public String getProducerId() {
        return this.producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public List<String> getConsumerIds() {
        return this.consumerIds;
    }

    public void setConsumerIds(List<String> consumerIds) {
        this.consumerIds = consumerIds;
    }

    public int getResultCode() {
        return this.resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }
}
