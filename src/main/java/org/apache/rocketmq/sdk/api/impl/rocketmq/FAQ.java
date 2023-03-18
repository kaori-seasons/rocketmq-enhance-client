package org.apache.rocketmq.sdk.api.impl.rocketmq;

public class FAQ {
    public static final String FIND_NS_FAILED = "http://#####";
    public static final String FIND_PS_FAILED = "http://#####";
    public static final String CONNECT_BROKER_FAILED = "http://#####";
    public static final String SEND_MSG_TO_BROKER_TIMEOUT = "http://#####";
    public static final String SERVICE_STATE_WRONG = "http://#####";
    public static final String BROKER_RESPONSE_EXCEPTION = "http://#####";
    public static final String CLIENT_CHECK_MSG_EXCEPTION = "http://#####";
    public static final String TOPIC_ROUTE_NOT_EXIST = "http://#####";

    public static String errorMessage(String msg, String url) {
        return String.format("%s\nSee %s for further details.", msg, url);
    }
}
