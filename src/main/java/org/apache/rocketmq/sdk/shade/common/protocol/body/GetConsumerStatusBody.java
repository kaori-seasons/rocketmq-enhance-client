package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class GetConsumerStatusBody extends RemotingSerializable {
    private Map<MessageQueue, Long> messageQueueTable = new HashMap();
    private Map<String, Map<MessageQueue, Long>> consumerTable = new HashMap();

    public Map<MessageQueue, Long> getMessageQueueTable() {
        return this.messageQueueTable;
    }

    public void setMessageQueueTable(Map<MessageQueue, Long> messageQueueTable) {
        this.messageQueueTable = messageQueueTable;
    }

    public Map<String, Map<MessageQueue, Long>> getConsumerTable() {
        return this.consumerTable;
    }

    public void setConsumerTable(Map<String, Map<MessageQueue, Long>> consumerTable) {
        this.consumerTable = consumerTable;
    }
}
