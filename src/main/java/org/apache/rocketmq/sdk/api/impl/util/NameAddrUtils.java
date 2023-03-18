package org.apache.rocketmq.sdk.api.impl.util;

import org.apache.rocketmq.sdk.shade.common.MixAll;

public class NameAddrUtils {
    public static String getNameAdd() {
        return System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY, System.getenv("NAMESRV_ADDR"));
    }
}
