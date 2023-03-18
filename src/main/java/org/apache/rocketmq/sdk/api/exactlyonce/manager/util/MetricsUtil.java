package org.apache.rocketmq.sdk.api.exactlyonce.manager.util;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;

public class MetricsUtil {
    public static void recordStart(MQTxContext context) {
        if (context != null) {
            context.setStartTimestamp(System.currentTimeMillis());
        }
    }

    public static void recordProcessTimeStamp(MQTxContext context) {
        if (context != null) {
            context.setProcessTimestamp(System.currentTimeMillis());
        }
    }

    public static void recordPersistenceTimestamp(MQTxContext context) {
        if (context != null) {
            context.setPersistenceTimestamp(System.currentTimeMillis());
        }
    }

    public static void recordAfterPersistenceTimestamp(MQTxContext context) {
        if (context != null) {
            context.setAftePersistenceTimestamp(System.currentTimeMillis());
        }
    }

    public static void recordFinishConsumeTimestamp(MQTxContext context) {
        if (context != null) {
            context.setFinishTimestamp(System.currentTimeMillis());
        }
    }

    public static long getProcessTime(MQTxContext context) {
        if (context == null) {
            return 0;
        }
        return context.getPersistenceTimestamp() - context.getProcessTimestamp();
    }

    public static long getPersistenceTime(MQTxContext context) {
        if (context == null) {
            return 0;
        }
        return context.getAftePersistenceTimestamp() - context.getPersistenceTimestamp();
    }

    public static long getConsumeTime(MQTxContext context) {
        if (context == null) {
            return 0;
        }
        return context.getFinishTimestamp() - context.getStartTimestamp();
    }
}
