package org.apache.rocketmq.sdk.shade.common.help;

public class FAQUrl {
    public static final String APPLY_TOPIC_URL = "http://rocketmq.apache.org/docs/faq/";
    public static final String NAME_SERVER_ADDR_NOT_EXIST_URL = "http://rocketmq.apache.org/docs/faq/";
    public static final String GROUP_NAME_DUPLICATE_URL = "http://rocketmq.apache.org/docs/faq/";
    public static final String CLIENT_PARAMETER_CHECK_URL = "http://rocketmq.apache.org/docs/faq/";
    public static final String SUBSCRIPTION_GROUP_NOT_EXIST = "http://rocketmq.apache.org/docs/faq/";
    public static final String CLIENT_SERVICE_NOT_OK = "http://rocketmq.apache.org/docs/faq/";
    public static final String NO_TOPIC_ROUTE_INFO = "http://rocketmq.apache.org/docs/faq/";
    public static final String LOAD_JSON_EXCEPTION = "http://rocketmq.apache.org/docs/faq/";
    public static final String SAME_GROUP_DIFFERENT_TOPIC = "http://rocketmq.apache.org/docs/faq/";
    public static final String MQLIST_NOT_EXIST = "http://rocketmq.apache.org/docs/faq/";
    public static final String UNEXPECTED_EXCEPTION_URL = "http://rocketmq.apache.org/docs/faq/";
    public static final String SEND_MSG_FAILED = "http://rocketmq.apache.org/docs/faq/";
    public static final String UNKNOWN_HOST_EXCEPTION = "http://rocketmq.apache.org/docs/faq/";
    private static final String TIP_STRING_BEGIN = "\nSee ";
    private static final String TIP_STRING_END = " for further details.";

    public static String suggestTodo(String url) {
        return TIP_STRING_BEGIN + url + TIP_STRING_END;
    }

    public static String attachDefaultURL(String errorMessage) {
        if (errorMessage == null || -1 != errorMessage.indexOf(TIP_STRING_BEGIN)) {
            return errorMessage;
        }
        return errorMessage + "\nFor more information, please visit the url, http://rocketmq.apache.org/docs/faq/";
    }
}
