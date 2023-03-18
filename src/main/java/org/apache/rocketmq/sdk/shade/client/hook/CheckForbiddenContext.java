package org.apache.rocketmq.sdk.shade.client.hook;

import org.apache.rocketmq.sdk.shade.client.impl.CommunicationMode;
import org.apache.rocketmq.sdk.shade.client.producer.SendResult;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.springframework.beans.PropertyAccessor;

public class CheckForbiddenContext {
    private String nameSrvAddr;
    private String group;
    private Message message;
    private MessageQueue mq;
    private String brokerAddr;
    private CommunicationMode communicationMode;
    private SendResult sendResult;
    private Exception exception;
    private Object arg;
    private boolean unitMode = false;

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Message getMessage() {
        return this.message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageQueue getMq() {
        return this.mq;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public String getBrokerAddr() {
        return this.brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public CommunicationMode getCommunicationMode() {
        return this.communicationMode;
    }

    public void setCommunicationMode(CommunicationMode communicationMode) {
        this.communicationMode = communicationMode;
    }

    public SendResult getSendResult() {
        return this.sendResult;
    }

    public void setSendResult(SendResult sendResult) {
        this.sendResult = sendResult;
    }

    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Object getArg() {
        return this.arg;
    }

    public void setArg(Object arg) {
        this.arg = arg;
    }

    public boolean isUnitMode() {
        return this.unitMode;
    }

    public void setUnitMode(boolean isUnitMode) {
        this.unitMode = isUnitMode;
    }

    public String getNameSrvAddr() {
        return this.nameSrvAddr;
    }

    public void setNameSrvAddr(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    public String toString() {
        return "SendMessageContext [nameSrvAddr=" + this.nameSrvAddr + ", group=" + this.group + ", message=" + this.message + ", mq=" + this.mq + ", brokerAddr=" + this.brokerAddr + ", communicationMode=" + this.communicationMode + ", sendResult=" + this.sendResult + ", exception=" + this.exception + ", unitMode=" + this.unitMode + ", arg=" + this.arg + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
