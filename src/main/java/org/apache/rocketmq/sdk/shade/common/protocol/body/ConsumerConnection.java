package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConsumerConnection extends RemotingSerializable {
    private HashSet<Connection> connectionSet = new HashSet<>();
    private ConcurrentMap<String, SubscriptionData> subscriptionTable = new ConcurrentHashMap();
    private ConsumeType consumeType;
    private MessageModel messageModel;
    private ConsumeFromWhere consumeFromWhere;

    public int computeMinVersion() {
        int minVersion = Integer.MAX_VALUE;
        Iterator<Connection> it = this.connectionSet.iterator();
        while (it.hasNext()) {
            Connection c = it.next();
            if (c.getVersion() < minVersion) {
                minVersion = c.getVersion();
            }
        }
        return minVersion;
    }

    public HashSet<Connection> getConnectionSet() {
        return this.connectionSet;
    }

    public void setConnectionSet(HashSet<Connection> connectionSet) {
        this.connectionSet = connectionSet;
    }

    public ConcurrentMap<String, SubscriptionData> getSubscriptionTable() {
        return this.subscriptionTable;
    }

    public void setSubscriptionTable(ConcurrentHashMap<String, SubscriptionData> subscriptionTable) {
        this.subscriptionTable = subscriptionTable;
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
}
