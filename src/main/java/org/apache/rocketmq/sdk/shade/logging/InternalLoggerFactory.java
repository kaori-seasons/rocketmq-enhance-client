package org.apache.rocketmq.sdk.shade.logging;

import java.util.concurrent.ConcurrentHashMap;

public abstract class InternalLoggerFactory {
    public static final String LOGGER_SLF4J = "slf4j";
    public static final String LOGGER_INNER = "inner";
    public static final String DEFAULT_LOGGER = "slf4j";
    private static String loggerType = null;
    private static ConcurrentHashMap<String, InternalLoggerFactory> loggerFactoryCache = new ConcurrentHashMap<>();

    protected abstract void shutdown();

    protected abstract InternalLogger getLoggerInstance(String str);

    protected abstract String getLoggerType();

    static {
        try {
            new Slf4jLoggerFactory();
        } catch (Throwable th) {
        }
        try {
            new InnerLoggerFactory();
        } catch (Throwable th2) {
        }
    }

    public static InternalLogger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    public static InternalLogger getLogger(String name) {
        return getLoggerFactory().getLoggerInstance(name);
    }

    private static InternalLoggerFactory getLoggerFactory() {
        InternalLoggerFactory internalLoggerFactory = null;
        if (loggerType != null) {
            internalLoggerFactory = loggerFactoryCache.get(loggerType);
        }
        if (internalLoggerFactory == null) {
            internalLoggerFactory = loggerFactoryCache.get("slf4j");
        }
        if (internalLoggerFactory == null) {
            internalLoggerFactory = loggerFactoryCache.get(LOGGER_INNER);
        }
        if (internalLoggerFactory != null) {
            return internalLoggerFactory;
        }
        throw new RuntimeException("[RocketMQ] Logger init failed, please check logger");
    }

    public static void setCurrentLoggerType(String type) {
        loggerType = type;
    }

    protected void doRegister() {
        String loggerType2 = getLoggerType();
        if (loggerFactoryCache.get(loggerType2) == null) {
            loggerFactoryCache.put(loggerType2, this);
        }
    }
}
