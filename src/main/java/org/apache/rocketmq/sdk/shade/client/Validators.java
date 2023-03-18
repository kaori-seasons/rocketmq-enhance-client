package org.apache.rocketmq.sdk.shade.client;

import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validators {
    public static final String VALID_PATTERN_STR = "^[%|a-zA-Z0-9_-]+$";
    public static final Pattern PATTERN = Pattern.compile(VALID_PATTERN_STR);
    public static final int CHARACTER_MAX_LENGTH = 255;

    public static String getGroupWithRegularExpression(String origin, String patternStr) {
        Matcher matcher = Pattern.compile(patternStr).matcher(origin);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    public static void checkGroup(String group) throws MQClientException {
        if (UtilAll.isBlank(group)) {
            throw new MQClientException("the specified group is blank", (Throwable) null);
        } else if (!regularExpressionMatcher(group, PATTERN)) {
            throw new MQClientException(String.format("the specified group[%s] contains illegal characters, allowing only %s", group, VALID_PATTERN_STR), (Throwable) null);
        } else if (group.length() > 255) {
            throw new MQClientException("the specified group is longer than group max length 255.", (Throwable) null);
        }
    }

    public static boolean regularExpressionMatcher(String origin, Pattern pattern) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(origin).matches();
    }

    public static void checkMessage(Message msg, DefaultMQProducer defaultMQProducer) throws MQClientException {
        if (null == msg) {
            throw new MQClientException(13, "the message is null");
        }
        checkTopic(msg.getTopic());
        if (null == msg.getBody()) {
            throw new MQClientException(13, "the message body is null");
        } else if (0 == msg.getBody().length) {
            throw new MQClientException(13, "the message body length is zero");
        } else if (msg.getBody().length > defaultMQProducer.getMaxMessageSize()) {
            throw new MQClientException(13, "the message body size over max value, MAX: " + defaultMQProducer.getMaxMessageSize());
        }
    }

    public static void checkTopic(String topic) throws MQClientException {
        if (UtilAll.isBlank(topic)) {
            throw new MQClientException("The specified topic is blank", (Throwable) null);
        } else if (!regularExpressionMatcher(topic, PATTERN)) {
            throw new MQClientException(String.format("The specified topic[%s] contains illegal characters, allowing only %s", topic, VALID_PATTERN_STR), (Throwable) null);
        } else if (topic.length() > 255) {
            throw new MQClientException("The specified topic is longer than topic max length 255.", (Throwable) null);
        } else if (topic.equals(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC)) {
            throw new MQClientException(String.format("The topic[%s] is conflict with AUTO_CREATE_TOPIC_KEY_TOPIC.", topic), (Throwable) null);
        }
    }
}
