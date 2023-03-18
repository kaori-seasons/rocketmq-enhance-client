package org.apache.rocketmq.sdk.api.impl.util;

import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Arrays;

public class ClientLoggerUtil {
    private static final String CLIENT_LOG_ROOT = "tuxedo.client.logRoot";
    private static final String CLIENT_LOG_FILEMAXINDEX = "tuxedo.client.logFileMaxIndex";
    private static final String CLIENT_LOG_LEVEL = "tuxedo.client.logLevel";
    private static final String[] levelArray = {"ERROR", "WARN", "INFO", "DEBUG"};
    private static final long CLIENT_LOG_FILESIZE = 67108864;

    public static InternalLogger getClientLogger() {
        int maxIndex = 0;
        System.setProperty(ClientLogger.CLIENT_LOG_ROOT, System.getProperty(CLIENT_LOG_ROOT, System.getProperty("user.home") + "/logs"));
        String tuxedoClientLogLevel = System.getProperty(CLIENT_LOG_LEVEL, "INFO").trim().toUpperCase();
        if (!Arrays.asList(levelArray).contains(tuxedoClientLogLevel)) {
            tuxedoClientLogLevel = "INFO";
        }
        System.setProperty(ClientLogger.CLIENT_LOG_LEVEL, tuxedoClientLogLevel);
        String tuxedoClientLogMaxIndex = System.getProperty(CLIENT_LOG_FILEMAXINDEX, "10").trim();
        try {
            maxIndex = Integer.parseInt(tuxedoClientLogMaxIndex);
        } catch (NumberFormatException e) {
            tuxedoClientLogMaxIndex = "10";
        }
        if (maxIndex <= 0 || maxIndex > 100) {
            throw new NumberFormatException();
        }
        System.setProperty(ClientLogger.CLIENT_LOG_MAXINDEX, tuxedoClientLogMaxIndex);
        System.setProperty(ClientLogger.CLIENT_LOG_FILENAME, "tuxedo.log");
        System.setProperty(ClientLogger.CLIENT_LOG_FILESIZE, String.valueOf((long) CLIENT_LOG_FILESIZE));
        return ClientLogger.getLog();
    }
}
