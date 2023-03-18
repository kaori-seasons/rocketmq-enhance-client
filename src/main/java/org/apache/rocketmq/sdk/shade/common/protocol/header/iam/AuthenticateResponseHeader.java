package org.apache.rocketmq.sdk.shade.common.protocol.header.iam;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

import java.util.List;

public class AuthenticateResponseHeader implements CommandCustomHeader {
    @CFNotNull
    private String sk_perm;
    private String producerId;
    private List<String> consumerIds;

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

    @Override
    public void checkFields() throws RemotingCommandException {
    }
}
