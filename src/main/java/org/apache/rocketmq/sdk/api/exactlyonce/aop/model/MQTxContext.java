package org.apache.rocketmq.sdk.api.exactlyonce.aop.model;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

public class MQTxContext {
    private String messageId;
    private String topicName;
    private String consumerGroup;
    private int timeoutMs;
    private int retryTimes;
    private long processTimestamp;
    private long persistenceTimestamp;
    private long aftePersistenceTimestamp;
    private long finishTimestamp;
    private MessageExt messageExt;
    private MessageQueue messageQueue;
    private Long offset;
    private TxStage stage;
    private TxStatus status;
    private DataSourceConfig dataSourceConfig;
    private boolean inTxEnv = false;
    private boolean inConsume = false;
    private boolean autoCommit = false;
    private boolean dup = false;
    private long startTimestamp = System.currentTimeMillis();

    public enum TxStage {
        BEFORE_TX,
        BEFORE_DB_COMMIT,
        COMMIT,
        ROLLBACK,
        AFTER_TX
    }

    public enum TxStatus {
        USER_PROCESS_FAIL,
        USER_DATA_FLUSH_DB_FAIL,
        TXRECORD_CONFLICT,
        PROCESS_OK
    }

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

    public boolean isInTxEnv() {
        return this.inTxEnv;
    }

    public void setInTxEnv(boolean inTxEnv) {
        this.inTxEnv = inTxEnv;
    }

    public boolean isInConsume() {
        return this.inConsume;
    }

    public boolean isAutoCommit() {
        return this.autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void setInConsume(boolean inConsume) {
        this.inConsume = inConsume;
    }

    public int getTimeoutMs() {
        return this.timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getRetryTimes() {
        return this.retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getProcessTimestamp() {
        return this.processTimestamp;
    }

    public void setProcessTimestamp(long processTimestamp) {
        this.processTimestamp = processTimestamp;
    }

    public long getPersistenceTimestamp() {
        return this.persistenceTimestamp;
    }

    public void setPersistenceTimestamp(long persistenceTimestamp) {
        this.persistenceTimestamp = persistenceTimestamp;
    }

    public long getAftePersistenceTimestamp() {
        return this.aftePersistenceTimestamp;
    }

    public void setAftePersistenceTimestamp(long aftePersistenceTimestamp) {
        this.aftePersistenceTimestamp = aftePersistenceTimestamp;
    }

    public long getFinishTimestamp() {
        return this.finishTimestamp;
    }

    public void setFinishTimestamp(long finishTimestamp) {
        this.finishTimestamp = finishTimestamp;
    }

    public MessageExt getMessageExt() {
        return this.messageExt;
    }

    public void setMessageExt(MessageExt messageExt) {
        this.messageExt = messageExt;
    }

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public Long getOffset() {
        return this.offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public TxStage getStage() {
        return this.stage;
    }

    public void setStage(TxStage stage) {
        this.stage = stage;
    }

    public TxStatus getStatus() {
        return this.status;
    }

    public void setStatus(TxStatus status) {
        this.status = status;
    }

    public DataSourceConfig getDataSourceConfig() {
        return this.dataSourceConfig;
    }

    public void setDataSourceConfig(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public boolean isDup() {
        return this.dup;
    }

    public void setDup(boolean dup) {
        this.dup = dup;
    }

    public static MQTxContext parseParam(MessageExt msgExt, String topicName, String consumerGroup) {
        MQTxContext context = new MQTxContext();
        if (msgExt == null) {
            return context;
        }
        context.setMessageId(msgExt.getMsgId());
        context.setConsumerGroup(consumerGroup);
        context.setTopicName(topicName);
        context.setOffset(Long.valueOf(msgExt.getQueueOffset()));
        return context;
    }

    public String toString() {
        return "MQTxContext{messageId='" + this.messageId + "', topicName='" + this.topicName + "', consumerGroup='" + this.consumerGroup + "', inTxEnv=" + this.inTxEnv + ", inConsume=" + this.inConsume + ", autoCommit=" + this.autoCommit + ", timeoutMs=" + this.timeoutMs + ", retryTimes=" + this.retryTimes + ", startTimestamp=" + this.startTimestamp + ", messageExt=" + this.messageExt + ", messageQueue=" + this.messageQueue + ", offset=" + this.offset + ", stage=" + this.stage + ", status=" + this.status + '}';
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MQTxContext)) {
            return false;
        }
        MQTxContext txContext = (MQTxContext) obj;
        boolean equal = false & (txContext.isInTxEnv() == this.inTxEnv);
        if (!equal) {
            return false;
        }
        if (!equal || !((txContext.getMessageId() == null && this.messageId == null) || !(txContext.getMessageId() == null || this.messageId == null || !this.messageId.equals(txContext.getMessageId())))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * (this.messageId != null ? this.messageId.hashCode() : 0)) + (this.topicName != null ? this.topicName.hashCode() : 0))) + (this.consumerGroup != null ? this.consumerGroup.hashCode() : 0))) + (this.inTxEnv ? 1 : 0))) + (this.inConsume ? 1 : 0))) + (this.autoCommit ? 1 : 0))) + this.timeoutMs)) + this.retryTimes)) + ((int) (this.startTimestamp ^ (this.startTimestamp >>> 32))))) + ((int) (this.processTimestamp ^ (this.processTimestamp >>> 32))))) + ((int) (this.persistenceTimestamp ^ (this.persistenceTimestamp >>> 32))))) + ((int) (this.aftePersistenceTimestamp ^ (this.aftePersistenceTimestamp >>> 32))))) + ((int) (this.finishTimestamp ^ (this.finishTimestamp >>> 32))))) + (this.messageExt != null ? this.messageExt.hashCode() : 0))) + (this.messageQueue != null ? this.messageQueue.hashCode() : 0))) + (this.offset != null ? this.offset.hashCode() : 0))) + (this.stage != null ? this.stage.hashCode() : 0))) + (this.status != null ? this.status.hashCode() : 0))) + (this.dataSourceConfig != null ? this.dataSourceConfig.hashCode() : 0);
    }
}
