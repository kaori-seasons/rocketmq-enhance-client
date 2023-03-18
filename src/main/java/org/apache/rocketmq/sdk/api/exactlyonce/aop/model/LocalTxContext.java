package org.apache.rocketmq.sdk.api.exactlyonce.aop.model;

public class LocalTxContext {
    private static final ThreadLocal<MQTxContext> MQTXCONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(MQTxContext transactionContext) {
        MQTXCONTEXT_THREAD_LOCAL.set(transactionContext);
    }

    public static void clear() {
        MQTXCONTEXT_THREAD_LOCAL.remove();
    }

    public static MQTxContext get() {
        return MQTXCONTEXT_THREAD_LOCAL.get();
    }
}
