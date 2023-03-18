package org.apache.rocketmq.sdk.shade.client.log;

import org.apache.rocketmq.sdk.shade.logging.InnerLoggerFactory;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.logging.inner.Appender;
import org.apache.rocketmq.sdk.shade.logging.inner.Level;
import org.apache.rocketmq.sdk.shade.logging.inner.Logger;
import org.apache.rocketmq.sdk.shade.logging.inner.LoggingBuilder;
import org.springframework.util.AntPathMatcher;

public class ClientLogger {
    public static final String CLIENT_LOG_ROOT = "rocketmq.client.logRoot";
    public static final String CLIENT_LOG_MAXINDEX = "rocketmq.client.logFileMaxIndex";
    public static final String CLIENT_LOG_FILESIZE = "rocketmq.client.logFileMaxSize";
    public static final String CLIENT_LOG_LEVEL = "rocketmq.client.logLevel";
    public static final String CLIENT_LOG_ADDITIVE = "rocketmq.client.log.additive";
    public static final String CLIENT_LOG_FILENAME = "rocketmq.client.logFileName";
    public static final String CLIENT_LOG_ASYNC_QUEUESIZE = "rocketmq.client.logAsyncQueueSize";
    public static final String ROCKETMQ_CLIENT_APPENDER_NAME = "RocketmqClientAppender";
    private static final InternalLogger CLIENT_LOGGER;
    private static Appender rocketmqClientAppender = null;
    public static final String CLIENT_LOG_USESLF4J = "rocketmq.client.logUseSlf4j";
    private static final boolean CLIENT_USE_SLF4J = Boolean.parseBoolean(System.getProperty(CLIENT_LOG_USESLF4J, "false"));

    static {
        if (!CLIENT_USE_SLF4J) {
            InternalLoggerFactory.setCurrentLoggerType("inner");
            CLIENT_LOGGER = createLogger("RocketmqClient");
            createLogger("RocketmqCommon");
            createLogger("RocketmqRemoting");
        } else {
            CLIENT_LOGGER = InternalLoggerFactory.getLogger("RocketmqClient");
        }
    }

    private static synchronized void createClientAppender() {
        String clientLogRoot = System.getProperty(CLIENT_LOG_ROOT, System.getProperty("user.home") + "/logs/rocketmqlogs");
        String clientLogMaxIndex = System.getProperty(CLIENT_LOG_MAXINDEX, "10");
        String clientLogFileName = System.getProperty(CLIENT_LOG_FILENAME, "rocketmq_client.log");
        String maxFileSize = System.getProperty(CLIENT_LOG_FILESIZE, "1073741824");
        String asyncQueueSize = System.getProperty(CLIENT_LOG_ASYNC_QUEUESIZE, "1024");
        String logFileName = clientLogRoot + AntPathMatcher.DEFAULT_PATH_SEPARATOR + clientLogFileName;
        int maxFileIndex = Integer.parseInt(clientLogMaxIndex);
        rocketmqClientAppender = LoggingBuilder.newAppenderBuilder().withRollingFileAppender(logFileName, maxFileSize, maxFileIndex).withAsync(false, Integer.parseInt(asyncQueueSize)).withName(ROCKETMQ_CLIENT_APPENDER_NAME).withLayout(LoggingBuilder.newLayoutBuilder().withDefaultLayout().build()).build();
        Logger.getRootLogger().addAppender(rocketmqClientAppender);
    }

    private static InternalLogger createLogger(String loggerName) {
        String clientLogLevel = System.getProperty(CLIENT_LOG_LEVEL, "INFO");
        boolean additive = "true".equalsIgnoreCase(System.getProperty(CLIENT_LOG_ADDITIVE));
        InternalLogger logger = InternalLoggerFactory.getLogger(loggerName);
        Logger realLogger = ((InnerLoggerFactory.InnerLogger) logger).getLogger();
        if (rocketmqClientAppender == null) {
            createClientAppender();
        }
        realLogger.addAppender(rocketmqClientAppender);
        realLogger.setLevel(Level.toLevel(clientLogLevel));
        realLogger.setAdditivity(additive);
        return logger;
    }

    public static InternalLogger getLog() {
        return CLIENT_LOGGER;
    }
}
