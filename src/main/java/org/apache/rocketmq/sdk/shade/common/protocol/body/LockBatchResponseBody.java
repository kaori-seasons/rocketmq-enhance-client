package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashSet;
import java.util.Set;

public class LockBatchResponseBody extends RemotingSerializable {
    private Set<MessageQueue> lockOKMQSet = new HashSet();

    public Set<MessageQueue> getLockOKMQSet() {
        return this.lockOKMQSet;
    }

    public void setLockOKMQSet(Set<MessageQueue> lockOKMQSet) {
        this.lockOKMQSet = lockOKMQSet;
    }
}
