package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.PropertyAccessor;

public class ConsumerData {
    private String groupName;
    private ConsumeType consumeType;
    private MessageModel messageModel;
    private ConsumeFromWhere consumeFromWhere;
    private Set<SubscriptionData> subscriptionDataSet = new HashSet();
    private boolean unitMode;

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ConsumeType getConsumeType() {
        return this.consumeType;
    }

    public void setConsumeType(ConsumeType consumeType) {
        this.consumeType = consumeType;
    }

    public MessageModel getMessageModel() {
        return this.messageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public ConsumeFromWhere getConsumeFromWhere() {
        return this.consumeFromWhere;
    }

    public void setConsumeFromWhere(ConsumeFromWhere consumeFromWhere) {
        this.consumeFromWhere = consumeFromWhere;
    }

    public Set<SubscriptionData> getSubscriptionDataSet() {
        return this.subscriptionDataSet;
    }

    public void setSubscriptionDataSet(Set<SubscriptionData> subscriptionDataSet) {
        this.subscriptionDataSet = subscriptionDataSet;
    }

    public boolean isUnitMode() {
        return this.unitMode;
    }

    public void setUnitMode(boolean isUnitMode) {
        this.unitMode = isUnitMode;
    }

    public String toString() {
        return "ConsumerData [groupName=" + this.groupName + ", consumeType=" + this.consumeType + ", messageModel=" + this.messageModel + ", consumeFromWhere=" + this.consumeFromWhere + ", unitMode=" + this.unitMode + ", subscriptionDataSet=" + this.subscriptionDataSet + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
