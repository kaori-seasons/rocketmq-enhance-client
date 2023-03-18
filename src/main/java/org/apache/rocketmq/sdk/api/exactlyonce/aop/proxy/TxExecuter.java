package org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy;

public interface TxExecuter {
    Object excute(InternalCallback internalCallback);

    Object excute(InternalCallback internalCallback, Object obj);

    void begin();

    void begin(int i);

    void commit();

    void rollback();
}
