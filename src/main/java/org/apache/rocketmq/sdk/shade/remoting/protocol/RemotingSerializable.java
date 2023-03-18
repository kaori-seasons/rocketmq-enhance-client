package org.apache.rocketmq.sdk.shade.remoting.protocol;

import com.alibaba.fastjson.JSON;
import java.nio.charset.Charset;

public abstract class RemotingSerializable {
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static byte[] encode(Object obj) {
        String json = toJson(obj, false);
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public static String toJson(Object obj, boolean prettyFormat) {
        return JSON.toJSONString(obj, prettyFormat);
    }

    public static <T> T decode(byte[] data, Class<T> classOfT) {
        return (T) fromJson(new String(data, CHARSET_UTF8), classOfT);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return (T) JSON.parseObject(json, classOfT);
    }

    public byte[] encode() {
        String json = toJson();
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public String toJson() {
        return toJson(false);
    }

    public String toJson(boolean prettyFormat) {
        return toJson(this, prettyFormat);
    }
}
