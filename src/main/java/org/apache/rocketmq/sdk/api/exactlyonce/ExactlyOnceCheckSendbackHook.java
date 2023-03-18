package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.DBAccessUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.TxContextUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.sdk.shade.client.hook.CheckSendBackHook;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

public class ExactlyOnceCheckSendbackHook implements CheckSendBackHook {
    private String consumerGroup;
    private DataSourceConfig config;

    public ExactlyOnceCheckSendbackHook(String consumerGroup, DataSourceConfig config) {
        this.consumerGroup = consumerGroup;
        this.config = config;
    }

    public DataSourceConfig getConfig() {
        return this.config;
    }

    public void setConfig(DataSourceConfig config) {
        this.config = config;
    }

    @Override 
    public String hookName() {
        return "ExactlyOnceCheckSendbackHook";
    }

    @Override 
    public boolean needSendBack(MessageExt msg, ConsumeConcurrentlyContext context) {
        if (this.consumerGroup == null || this.config == null) {
            return true;
        }
        MQTxContext txContext = new MQTxContext();
        txContext.setMessageId(TxContextUtil.buildInternalMsgId(msg, this.consumerGroup));
        txContext.setDataSourceConfig(this.config);
        return !DBAccessUtil.isRecordExist(txContext);
    }
}
