package org.apache.rocketmq.sdk.shade.common.filter;

public class ExpressionType {
    public static final String SQL92 = "SQL92";
    public static final String TAG = "TAG";

    public static boolean isTagType(String type) {
        if (type == null || "".equals(type) || TAG.equals(type)) {
            return true;
        }
        return false;
    }
}
