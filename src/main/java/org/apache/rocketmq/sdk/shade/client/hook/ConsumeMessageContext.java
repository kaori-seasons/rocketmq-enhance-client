package org.apache.rocketmq.sdk.shade.client.hook;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.List;
import java.util.Map;

public class ConsumeMessageContext {
    private String consumerGroup;
    private List<MessageExt> msgList;
    private MessageQueue mq;
    private boolean success;
    private String status;
    private Object mqTraceContext;
    private Map<String, String> props;
    private String namespace;

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public List<MessageExt> getMsgList() {
        return this.msgList;
    }

    public void setMsgList(List<MessageExt> msgList) {
        this.msgList = msgList;
    }

    public MessageQueue getMq() {
        return this.mq;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getMqTraceContext() {
        return this.mqTraceContext;
    }

    public void setMqTraceContext(Object mqTraceContext) {
        this.mqTraceContext = mqTraceContext;
    }

    public Map<String, String> getProps() {
        return this.props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
