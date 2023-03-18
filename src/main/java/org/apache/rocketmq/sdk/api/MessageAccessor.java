package org.apache.rocketmq.sdk.api;

import java.util.Properties;

public class MessageAccessor {
    public static Properties getSystemProperties(Message msg) {
        return msg.systemProperties;
    }

    public static void setSystemProperties(Message msg, Properties systemProperties) {
        msg.systemProperties = systemProperties;
    }

    public static void putSystemProperties(Message msg, String key, String value) {
        msg.putSystemProperties(key, value);
    }

    public static String getSystemProperties(Message msg, String key) {
        return msg.getSystemProperties(key);
    }
}
