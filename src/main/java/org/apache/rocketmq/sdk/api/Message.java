package org.apache.rocketmq.sdk.api;

import java.io.Serializable;
import java.util.Properties;
import org.springframework.beans.PropertyAccessor;

public class Message implements Serializable {
    private static final long serialVersionUID = -1385924226856188094L;
    Properties systemProperties;
    private String topic;
    private Properties userProperties;
    private byte[] body;

    public static class SystemPropKey {
        public static final String TAG = "__TAG";
        public static final String KEY = "__KEY";
        public static final String MSGID = "__MSGID";
        public static final String SHARDINGKEY = "__SHARDINGKEY";
        public static final String RECONSUMETIMES = "__RECONSUMETIMES";
        public static final String BORNTIMESTAMP = "__BORNTIMESTAMP";
        public static final String BORNHOST = "__BORNHOST";
        public static final String STARTDELIVERTIME = "__STARTDELIVERTIME";
    }

    public Message() {
        this(null, null, "", null);
    }

    public Message(String topic, String tag, String key, byte[] body) {
        this.topic = topic;
        this.body = body;
        putSystemProperties(SystemPropKey.TAG, tag);
        putSystemProperties(SystemPropKey.KEY, key);
    }

    public void putSystemProperties(String key, String value) {
        if (null == this.systemProperties) {
            this.systemProperties = new Properties();
        }
        if (key != null && value != null) {
            this.systemProperties.put(key, value);
        }
    }

    public Message(String topic, String tags, byte[] body) {
        this(topic, tags, "", body);
    }

    public void putUserProperties(String key, String value) {
        if (null == this.userProperties) {
            this.userProperties = new Properties();
        }
        if (key != null && value != null) {
            this.userProperties.put(key, value);
        }
    }

    public String getUserProperties(String key) {
        if (null != this.userProperties) {
            return (String) this.userProperties.get(key);
        }
        return null;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return getSystemProperties(SystemPropKey.TAG);
    }

    public String getSystemProperties(String key) {
        if (null != this.systemProperties) {
            return this.systemProperties.getProperty(key);
        }
        return null;
    }

    public void setTag(String tag) {
        putSystemProperties(SystemPropKey.TAG, tag);
    }

    public String getKey() {
        return getSystemProperties(SystemPropKey.KEY);
    }

    public void setKey(String key) {
        putSystemProperties(SystemPropKey.KEY, key);
    }

    public String getMsgID() {
        return getSystemProperties(SystemPropKey.MSGID);
    }

    public void setMsgID(String msgid) {
        putSystemProperties(SystemPropKey.MSGID, msgid);
    }

    Properties getSystemProperties() {
        return this.systemProperties;
    }

    void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public Properties getUserProperties() {
        return this.userProperties;
    }

    public void setUserProperties(Properties userProperties) {
        this.userProperties = userProperties;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getReconsumeTimes() {
        String pro = getSystemProperties(SystemPropKey.RECONSUMETIMES);
        if (pro != null) {
            return Integer.parseInt(pro);
        }
        return 0;
    }

    public void setReconsumeTimes(int value) {
        putSystemProperties(SystemPropKey.RECONSUMETIMES, String.valueOf(value));
    }

    public long getBornTimestamp() {
        String pro = getSystemProperties(SystemPropKey.BORNTIMESTAMP);
        if (pro != null) {
            return Long.parseLong(pro);
        }
        return 0;
    }

    public void setBornTimestamp(long value) {
        putSystemProperties(SystemPropKey.BORNTIMESTAMP, String.valueOf(value));
    }

    public String getBornHost() {
        String pro = getSystemProperties(SystemPropKey.BORNHOST);
        return pro == null ? "" : pro;
    }

    public void setBornHost(String value) {
        putSystemProperties(SystemPropKey.BORNHOST, value);
    }

    public long getStartDeliverTime() {
        String pro = getSystemProperties(SystemPropKey.STARTDELIVERTIME);
        if (pro != null) {
            return Long.parseLong(pro);
        }
        return 0;
    }

    public String getShardingKey() {
        String pro = getSystemProperties(SystemPropKey.SHARDINGKEY);
        return pro == null ? "" : pro;
    }

    public void setShardingKey(String value) {
        putSystemProperties(SystemPropKey.SHARDINGKEY, value);
    }

    public void setStartDeliverTime(long value) {
        putSystemProperties(SystemPropKey.STARTDELIVERTIME, String.valueOf(value));
    }

    @Override
    public String toString() {
        return "Message [topic=" + this.topic + ", systemProperties=" + this.systemProperties + ", userProperties=" + this.userProperties + ", body=" + (this.body != null ? this.body.length : 0) + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
