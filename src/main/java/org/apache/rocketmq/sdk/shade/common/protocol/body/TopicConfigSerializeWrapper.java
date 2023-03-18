package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.DataVersion;
import org.apache.rocketmq.sdk.shade.common.TopicConfig;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TopicConfigSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<String, TopicConfig> topicConfigTable = new ConcurrentHashMap();
    private DataVersion dataVersion = new DataVersion();

    public ConcurrentMap<String, TopicConfig> getTopicConfigTable() {
        return this.topicConfigTable;
    }

    public void setTopicConfigTable(ConcurrentMap<String, TopicConfig> topicConfigTable) {
        this.topicConfigTable = topicConfigTable;
    }

    public DataVersion getDataVersion() {
        return this.dataVersion;
    }

    public void setDataVersion(DataVersion dataVersion) {
        this.dataVersion = dataVersion;
    }
}
