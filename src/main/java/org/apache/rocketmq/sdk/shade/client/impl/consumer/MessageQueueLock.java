package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageQueueLock {
    private ConcurrentMap<MessageQueue, Object> mqLockTable = new ConcurrentHashMap();

    public Object fetchLockObject(MessageQueue mq) {
        Object objLock = this.mqLockTable.get(mq);
        if (null == objLock) {
            objLock = new Object();
            Object prevLock = this.mqLockTable.putIfAbsent(mq, objLock);
            if (prevLock != null) {
                objLock = prevLock;
            }
        }
        return objLock;
    }
}
