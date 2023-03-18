package org.apache.rocketmq.sdk.shade.common.namesrv;

import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.utils.HttpTinyClient;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public class TopAddressing {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    private String nsAddr;
    private String wsAddr;
    private String unitName;

    public TopAddressing(String wsAddr) {
        this(wsAddr, null);
    }

    public TopAddressing(String wsAddr, String unitName) {
        this.wsAddr = wsAddr;
        this.unitName = unitName;
    }

    private static String clearNewLine(String str) {
        String newString = str.trim();
        int index = newString.indexOf(StringUtils.CR);
        if (index != -1) {
            return newString.substring(0, index);
        }
        int index2 = newString.indexOf("\n");
        if (index2 != -1) {
            return newString.substring(0, index2);
        }
        return newString;
    }

    public final String fetchNSAddr() {
        return fetchNSAddr(true, 3000);
    }

    public final String fetchNSAddr(boolean verbose, long timeoutMills) {
        String url = this.wsAddr;
        try {
            if (!UtilAll.isBlank(this.unitName)) {
                url = url + "-" + this.unitName + "?nofix=1";
            }
            HttpTinyClient.HttpResult result = HttpTinyClient.httpGet(url, null, null, "UTF-8", timeoutMills);
            if (200 == result.code) {
                String responseStr = result.content;
                if (responseStr != null) {
                    return clearNewLine(responseStr);
                }
                log.error("fetch nameserver address is null");
            } else {
                log.error("fetch nameserver address failed. statusCode=" + result.code);
            }
        } catch (IOException e) {
            if (verbose) {
                log.error("fetch name server address exception", (Throwable) e);
            }
        }
        if (!verbose) {
            return null;
        }
        log.warn(("connect to " + url + " failed, maybe the domain name " + MixAll.getWSAddr() + " not bind in /etc/hosts") + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"));
        return null;
    }

    public String getNsAddr() {
        return this.nsAddr;
    }

    public void setNsAddr(String nsAddr) {
        this.nsAddr = nsAddr;
    }
}
