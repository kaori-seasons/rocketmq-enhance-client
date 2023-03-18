package org.apache.rocketmq.sdk.api.exactlyonce.aop.model;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;

public class MQTxRecord {
    private String messageId;
    private String topicName;
    private String consumerGroup;
    private String brokerName;
    private int qid;
    private long offset;
    private Long createTime;
    private DataSourceConfig dataSourceConfig;
    private long id;

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQid() {
        return this.qid;
    }

    public void setQid(int qid) {
        this.qid = qid;
    }

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public static boolean validate(MQTxRecord redoLog) {
        return true;
    }

    public Long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public DataSourceConfig getDataSourceConfig() {
        return this.dataSourceConfig;
    }

    public void setDataSourceConfig(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String toString() {
        return "MQTxRecord{messageId='" + this.messageId + "', topicName='" + this.topicName + "', consumerGroup='" + this.consumerGroup + "', brokerName='" + this.brokerName + "', qid=" + this.qid + ", offset=" + this.offset + ", createTime=" + this.createTime + ", dataSourceConfig=" + this.dataSourceConfig + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MQTxRecord record = (MQTxRecord) o;
        if (this.qid == record.qid && this.offset == record.offset && this.messageId.equals(record.messageId) && this.topicName.equals(record.topicName) && this.consumerGroup.equals(record.consumerGroup) && this.brokerName.equals(record.brokerName)) {
            return this.createTime.equals(record.createTime);
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * this.messageId.hashCode()) + this.topicName.hashCode())) + this.consumerGroup.hashCode())) + this.brokerName.hashCode())) + this.qid)) + ((int) (this.offset ^ (this.offset >>> 32))))) + this.createTime.hashCode();
    }
}
