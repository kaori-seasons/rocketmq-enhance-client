package org.apache.rocketmq.sdk.api.exactlyonce.manager;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.impl.TxRecordManagerImpl;

import java.util.Properties;

public class TxRecordManager {
    private TxRecordManagerImpl recordManagerImpl;

    public TxRecordManager(Properties properties) {
        this.recordManagerImpl = new TxRecordManagerImpl(properties);
    }

    public void start() {
        this.recordManagerImpl.start();
    }

    public void stop() {
        this.recordManagerImpl.stop();
    }

    public void flushTxRecord(MQTxConnection connection, MQTxContext context) throws Exception {
        this.recordManagerImpl.flushTxRecord(connection, context);
    }
}
