package org.apache.rocketmq.sdk.api.exactlyonce.manager.util;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxRecord;
import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeExactlyOnceStatus;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

public class TxContextUtil {
    public static void updateTxContext(MQTxContext txContext) {
        if (txContext == null) {
            txContext = buildTxContext();
        }
        txContext.setInTxEnv(true);
        txContext.setAutoCommit(true);
    }

    public static MQTxContext buildTxContext() {
        MQTxContext txContext = new MQTxContext();
        txContext.setInTxEnv(true);
        txContext.setAutoCommit(true);
        return txContext;
    }

    public static MQTxRecord buildTxRecord(MQTxContext context) {
        MQTxRecord record = new MQTxRecord();
        record.setTopicName(context.getTopicName());
        record.setOffset(context.getOffset().longValue());
        record.setMessageId(context.getMessageId());
        record.setQid(context.getMessageQueue().getQueueId());
        record.setBrokerName(context.getMessageQueue().getBrokerName());
        record.setConsumerGroup(context.getConsumerGroup());
        record.setCreateTime(Long.valueOf(System.currentTimeMillis()));
        return record;
    }

    public static boolean isTxContextCommitted(MQTxContext context) {
        return context != null && context.getStage() != null && MQTxContext.TxStage.COMMIT == context.getStage() && MQTxContext.TxStatus.PROCESS_OK == context.getStatus();
    }

    public static boolean isTxContextRollbacked(MQTxContext context) {
        return context != null && MQTxContext.TxStage.ROLLBACK == context.getStage() && MQTxContext.TxStatus.USER_PROCESS_FAIL == context.getStatus();
    }

    public static boolean isTxContextFinished(MQTxContext context) {
        if (context == null) {
            return false;
        }
        return (MQTxContext.TxStage.COMMIT == context.getStage() && MQTxContext.TxStatus.PROCESS_OK == context.getStatus()) || (MQTxContext.TxStage.ROLLBACK == context.getStage() && MQTxContext.TxStatus.USER_PROCESS_FAIL == context.getStatus());
    }

    public static boolean isTxProcessed(MQTxContext context) {
        return (context == null || context.getStage() == null || context.getStatus() == null) ? false : true;
    }

    public static String buildInternalMsgId(MessageExt msg, String consumerGroup) {
        String uniqInfo = null;
        if (msg.getProperty(MessageConst.PROPERTY_EXTEND_UNIQ_INFO) != null) {
            uniqInfo = msg.getProperty(MessageConst.PROPERTY_EXTEND_UNIQ_INFO);
        } else if (msg.getMsgId() != null) {
            uniqInfo = msg.getMsgId();
        }
        return uniqInfo + "#" + consumerGroup;
    }

    public static ConsumeExactlyOnceStatus getExactlyOnceStatus(MQTxContext context) {
        if (!isTxProcessed(context)) {
            return ConsumeExactlyOnceStatus.NO_EXACTLYONCE;
        }
        if (context.isDup()) {
            return ConsumeExactlyOnceStatus.EXACTLYONCE_DEDUP;
        }
        return ConsumeExactlyOnceStatus.EXACTLYONCE_PASS;
    }
}
