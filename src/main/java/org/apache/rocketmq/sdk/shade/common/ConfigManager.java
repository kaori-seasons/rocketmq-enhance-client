package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

import java.io.IOException;

public abstract class ConfigManager {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    public abstract String encode();

    public abstract String configFilePath();

    public abstract void decode(String str);

    public abstract String encode(boolean z);

    public boolean load() {
        try {
            String fileName = configFilePath();
            String jsonString = MixAll.file2String(fileName);
            if (null == jsonString || jsonString.length() == 0) {
                return loadBak();
            }
            decode(jsonString);
            log.info("load " + fileName + " OK");
            return true;
        } catch (Exception e) {
            log.error("load " + ((String) null) + " failed, and try to load backup file", (Throwable) e);
            return loadBak();
        }
    }

    private boolean loadBak() {
        String fileName = null;
        try {
            fileName = configFilePath();
            String jsonString = MixAll.file2String(fileName + ".bak");
            if (jsonString == null || jsonString.length() <= 0) {
                return true;
            }
            decode(jsonString);
            log.info("load " + fileName + " OK");
            return true;
        } catch (Exception e) {
            log.error("load " + fileName + " Failed", (Throwable) e);
            return false;
        }
    }

    public synchronized void persist() {
        String jsonString = encode(true);
        if (jsonString != null) {
            String fileName = configFilePath();
            try {
                MixAll.string2File(jsonString, fileName);
            } catch (IOException e) {
                log.error("persist file " + fileName + " exception", (Throwable) e);
            }
        }
    }
}
