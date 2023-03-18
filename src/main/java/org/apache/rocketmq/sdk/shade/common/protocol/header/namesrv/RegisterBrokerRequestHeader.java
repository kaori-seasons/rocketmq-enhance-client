package org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class RegisterBrokerRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String brokerName;
    @CFNotNull
    private String brokerAddr;
    @CFNotNull
    private String clusterName;
    @CFNotNull
    private String haServerAddr;
    @CFNotNull
    private Long brokerId;
    private boolean compressed;
    private Integer bodyCrc32 = 0;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerAddr() {
        return this.brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getHaServerAddr() {
        return this.haServerAddr;
    }

    public void setHaServerAddr(String haServerAddr) {
        this.haServerAddr = haServerAddr;
    }

    public Long getBrokerId() {
        return this.brokerId;
    }

    public void setBrokerId(Long brokerId) {
        this.brokerId = brokerId;
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public Integer getBodyCrc32() {
        return this.bodyCrc32;
    }

    public void setBodyCrc32(Integer bodyCrc32) {
        this.bodyCrc32 = bodyCrc32;
    }
}
