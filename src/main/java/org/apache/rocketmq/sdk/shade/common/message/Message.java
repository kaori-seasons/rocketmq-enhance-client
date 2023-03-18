package org.apache.rocketmq.sdk.shade.common.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 8445773977080406428L;
    private String topic;
    private int flag;
    private Map<String, String> properties;
    private byte[] body;
    private String transactionId;

    public Message() {
    }

    public Message(String topic, byte[] body) {
        this(topic, "", "", 0, body, true);
    }

    public Message(String topic, String tags, String keys, int flag, byte[] body, boolean waitStoreMsgOK) {
        this.topic = topic;
        this.flag = flag;
        this.body = body;
        if (tags != null && tags.length() > 0) {
            setTags(tags);
        }
        if (keys != null && keys.length() > 0) {
            setKeys(keys);
        }
        setWaitStoreMsgOK(waitStoreMsgOK);
    }

    public Message(String topic, String tags, byte[] body) {
        this(topic, tags, "", 0, body, true);
    }

    public Message(String topic, String tags, String keys, byte[] body) {
        this(topic, tags, keys, 0, body, true);
    }

    public void setKeys(String keys) {
        putProperty(MessageConst.PROPERTY_KEYS, keys);
    }

    public void putProperty(String name, String value) {
        if (null == this.properties) {
            this.properties = new HashMap();
        }
        this.properties.put(name, value);
    }

    public void clearProperty(String name) {
        if (null != this.properties) {
            this.properties.remove(name);
        }
    }

    public void putUserProperty(String name, String value) {
        if (MessageConst.STRING_HASH_SET.contains(name)) {
            throw new RuntimeException(String.format("The Property<%s> is used by system, input another please", name));
        } else if (value == null || value.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("The name or value of property can not be null or blank string!");
        } else {
            putProperty(name, value);
        }
    }

    public String getUserProperty(String name) {
        return getProperty(name);
    }

    public String getProperty(String name) {
        if (null == this.properties) {
            this.properties = new HashMap();
        }
        return this.properties.get(name);
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTags() {
        return getProperty(MessageConst.PROPERTY_TAGS);
    }

    public void setTags(String tags) {
        putProperty(MessageConst.PROPERTY_TAGS, tags);
    }

    public String getKeys() {
        return getProperty(MessageConst.PROPERTY_KEYS);
    }

    public void setKeys(Collection<String> keys) {
        StringBuffer sb = new StringBuffer();
        for (String k : keys) {
            sb.append(k);
            sb.append(" ");
        }
        setKeys(sb.toString().trim());
    }

    public int getDelayTimeLevel() {
        String t = getProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        if (t != null) {
            return Integer.parseInt(t);
        }
        return 0;
    }

    public void setDelayTimeLevel(int level) {
        putProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL, String.valueOf(level));
    }

    public boolean isWaitStoreMsgOK() {
        String result = getProperty(MessageConst.PROPERTY_WAIT_STORE_MSG_OK);
        if (null == result) {
            return true;
        }
        return Boolean.parseBoolean(result);
    }

    public void setWaitStoreMsgOK(boolean waitStoreMsgOK) {
        putProperty(MessageConst.PROPERTY_WAIT_STORE_MSG_OK, Boolean.toString(waitStoreMsgOK));
    }

    public void setInstanceId(String instanceId) {
        putProperty("INSTANCE_ID", instanceId);
    }

    public int getFlag() {
        return this.flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getBuyerId() {
        return getProperty(MessageConst.PROPERTY_BUYER_ID);
    }

    public void setBuyerId(String buyerId) {
        putProperty(MessageConst.PROPERTY_BUYER_ID, buyerId);
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "Message{topic='" + this.topic + "', flag=" + this.flag + ", properties=" + this.properties + ", body=" + Arrays.toString(this.body) + ", transactionId='" + this.transactionId + "'}";
    }
}
