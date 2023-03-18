package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class QueryMessageRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String topic;
    @CFNotNull
    private String key;
    @CFNotNull
    private Integer maxNum;
    @CFNotNull
    private Long beginTimestamp;
    @CFNotNull
    private Long endTimestamp;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getMaxNum() {
        return this.maxNum;
    }

    public void setMaxNum(Integer maxNum) {
        this.maxNum = maxNum;
    }

    public Long getBeginTimestamp() {
        return this.beginTimestamp;
    }

    public void setBeginTimestamp(Long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public Long getEndTimestamp() {
        return this.endTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}
