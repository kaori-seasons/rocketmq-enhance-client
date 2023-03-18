package org.apache.rocketmq.sdk.trace.core.common;

import org.apache.rocketmq.sdk.shade.common.message.MessageType;
import org.apache.rocketmq.sdk.trace.core.utils.MixUtils;

public class RMQTraceBean {
    private static final String LOCAL_ADDRESS = MixUtils.getLocalAddress();
    private String topic = "";
    private String msgId = "";
    private String offsetMsgId = "";
    private String tags = "";
    private String keys = "";
    private String storeHost = LOCAL_ADDRESS;
    private String clientHost = LOCAL_ADDRESS;
    private long storeTime;
    private long bornTime;
    private int retryTimes;
    private int bodyLength;
    private MessageType msgType;

    public MessageType getMsgType() {
        return this.msgType;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public String getOffsetMsgId() {
        return this.offsetMsgId;
    }

    public void setOffsetMsgId(String offsetMsgId) {
        this.offsetMsgId = offsetMsgId;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTags() {
        return this.tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getKeys() {
        return this.keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getStoreHost() {
        return this.storeHost;
    }

    public void setStoreHost(String storeHost) {
        this.storeHost = storeHost;
    }

    public String getClientHost() {
        return this.clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public long getStoreTime() {
        return this.storeTime;
    }

    public void setStoreTime(long storeTime) {
        this.storeTime = storeTime;
    }

    public int getRetryTimes() {
        return this.retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getBodyLength() {
        return this.bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public long getBornTime() {
        return this.bornTime;
    }

    public void setBornTime(long bornTime) {
        this.bornTime = bornTime;
    }
}
