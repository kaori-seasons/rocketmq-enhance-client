package org.apache.rocketmq.sdk.api.impl.util;

import org.apache.rocketmq.sdk.api.MessageAccessor;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.lang.reflect.Field;
import java.util.*;

public class RMQUtil {
    private static final Set<String> ReservedKeySetRMQ = new HashSet();
    private static final Set<String> ReservedKeySetSystemEnv = new HashSet();

    static {
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_KEYS);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_TAGS);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_WAIT_STORE_MSG_OK);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_RETRY_TOPIC);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_REAL_TOPIC);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_REAL_QUEUE_ID);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_TRANSACTION_PREPARED);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_PRODUCER_GROUP);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_MIN_OFFSET);
        ReservedKeySetRMQ.add(MessageConst.PROPERTY_MAX_OFFSET);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.TAG);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.KEY);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.MSGID);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.RECONSUMETIMES);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.STARTDELIVERTIME);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.BORNHOST);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.BORNTIMESTAMP);
        ReservedKeySetSystemEnv.add(org.apache.rocketmq.sdk.api.Message.SystemPropKey.SHARDINGKEY);
    }

    public static org.apache.rocketmq.sdk.api.Message msgConvert(Message msgRMQ) {
        org.apache.rocketmq.sdk.api.Message message = new org.apache.rocketmq.sdk.api.Message();
        if (msgRMQ.getTopic() != null) {
            message.setTopic(msgRMQ.getTopic());
        }

        if (msgRMQ.getKeys() != null) {
            message.setKey(msgRMQ.getKeys());
        }

        if (msgRMQ.getTags() != null) {
            message.setTag(msgRMQ.getTags());
        }

        if (msgRMQ.getBody() != null) {
            message.setBody(msgRMQ.getBody());
        }

        message.setReconsumeTimes(((MessageExt)msgRMQ).getReconsumeTimes());
        message.setBornTimestamp(((MessageExt)msgRMQ).getBornTimestamp());
        message.setBornHost(String.valueOf(((MessageExt)msgRMQ).getBornHost()));
        Map<String, String> properties = msgRMQ.getProperties();
        if (properties != null) {
            Iterator<Map.Entry<String, String>> it = properties.entrySet().iterator();

            while(true) {
                while(it.hasNext()) {
                    Map.Entry<String, String> next = (Map.Entry)it.next();
                    if (!ReservedKeySetRMQ.contains(next.getKey()) && !ReservedKeySetSystemEnv.contains(next.getKey())) {
                        message.putUserProperties((String)next.getKey(), (String)next.getValue());
                    } else {
                        MessageAccessor.putSystemProperties(message, (String)next.getKey(), (String)next.getValue());
                    }
                }

                return message;
            }
        } else {
            return message;
        }
    }


    public static Message msgConvert(org.apache.rocketmq.sdk.api.Message message) {
        Message msgRMQ = new Message();
        if (message == null) {
            throw new RMQClientException("'message' is null");
        } else {
            if (message.getTopic() != null) {
                msgRMQ.setTopic(message.getTopic());
            }

            if (message.getKey() != null) {
                msgRMQ.setKeys(message.getKey());
            }

            if (message.getTag() != null) {
                msgRMQ.setTags(message.getTag());
            }

            if (message.getStartDeliverTime() > 0L) {
                msgRMQ.putUserProperty("__STARTDELIVERTIME", String.valueOf(message.getStartDeliverTime()));
            }

            if (message.getBody() != null) {
                msgRMQ.setBody(message.getBody());
            }

            if (message.getShardingKey() != null && !message.getShardingKey().isEmpty()) {
                msgRMQ.putUserProperty("__SHARDINGKEY", message.getShardingKey());
            }

            Properties systemProperties = MessageAccessor.getSystemProperties(message);
            if (systemProperties != null) {
                Iterator<Map.Entry<Object, Object>> it = systemProperties.entrySet().iterator();

                while(it.hasNext()) {
                    Map.Entry<Object, Object> next = (Map.Entry)it.next();
                    if (!ReservedKeySetSystemEnv.contains(next.getKey().toString())) {
                        org.apache.rocketmq.sdk.shade.common.message.MessageAccessor.putProperty(msgRMQ, next.getKey().toString(), next.getValue().toString());
                    }
                }
            }

            Properties userProperties = message.getUserProperties();
            if (userProperties != null) {
                Iterator<Map.Entry<Object, Object>> it = userProperties.entrySet().iterator();

                while(it.hasNext()) {
                    Map.Entry<Object, Object> next = (Map.Entry)it.next();
                    if (!ReservedKeySetRMQ.contains(next.getKey().toString())) {
                        org.apache.rocketmq.sdk.shade.common.message.MessageAccessor.putProperty(msgRMQ, next.getKey().toString(), next.getValue().toString());
                    }
                }
            }

            return msgRMQ;
        }
    }

    public static Properties extractProperties(Properties properties) {
        Properties newPro = new Properties();
        Properties inner = null;
        try {
            Field field = Properties.class.getDeclaredField("defaults");
            field.setAccessible(true);
            inner = (Properties) field.get(properties);
        } catch (Exception e) {
        }
        if (inner != null) {
            for (Map.Entry<Object, Object> entry : inner.entrySet()) {
                newPro.setProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        for (Map.Entry<Object, Object> entry2 : properties.entrySet()) {
            newPro.setProperty(String.valueOf(entry2.getKey()), String.valueOf(entry2.getValue()));
        }
        return newPro;
    }
}
