package org.apache.rocketmq.sdk.api.impl;

import org.apache.rocketmq.sdk.shade.common.MQVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MQClientInfo {
    public static int versionCode;

    static {
        versionCode = MQVersion.CURRENT_VERSION;
        InputStream stream = null;
        try {
            stream = MQClientInfo.class.getClassLoader().getResourceAsStream("ons_client_info.properties");
            Properties properties = new Properties();
            properties.load(stream);
            versionCode = Integer.MAX_VALUE - Integer.valueOf(String.valueOf(properties.get("version")).replaceAll("[^0-9]", "")).intValue();
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        } catch (Exception e2) {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }
}
