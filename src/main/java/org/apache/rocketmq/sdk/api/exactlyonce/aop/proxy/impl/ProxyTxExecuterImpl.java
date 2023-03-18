package org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.impl;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.annotation.MQTransaction;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.InternalCallback;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.MetricsUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.TxContextUtil;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;

public class ProxyTxExecuterImpl {
    public Object excute(InternalCallback callback) {
        return excute(callback, null);
    }

    public Object excute(InternalCallback callback, Object object) {
        if (callback == null) {
            throw new RMQClientException("consume method not found");
        }
        if (object != null) {
            begin(((MQTransaction) object).timeout());
        } else {
            begin();
        }
        Object result = callback.run();
        MetricsUtil.recordFinishConsumeTimestamp(LocalTxContext.get());
        return result;
    }

    public void begin() {
        begin(-1);
    }

    public void begin(int timeout) {
        MQTxContext txContext = LocalTxContext.get();
        if (txContext == null) {
            txContext = TxContextUtil.buildTxContext();
        } else {
            TxContextUtil.updateTxContext(txContext);
        }
        if (0 < timeout && 600000 >= timeout) {
            txContext.setTimeoutMs(timeout);
        }
        txContext.setInTxEnv(true);
        MetricsUtil.recordProcessTimeStamp(txContext);
        LocalTxContext.set(txContext);
    }

    public void commit() {
        MQTxContext context = LocalTxContext.get();
        if (!TxContextUtil.isTxContextFinished(context)) {
            context.setStage(MQTxContext.TxStage.COMMIT);
            context.setStatus(MQTxContext.TxStatus.PROCESS_OK);
        }
    }

    public void rollback() {
        MQTxContext context = LocalTxContext.get();
        if (!TxContextUtil.isTxContextFinished(context)) {
            context.setStage(MQTxContext.TxStage.ROLLBACK);
            context.setStatus(MQTxContext.TxStatus.USER_PROCESS_FAIL);
        }
    }
}
