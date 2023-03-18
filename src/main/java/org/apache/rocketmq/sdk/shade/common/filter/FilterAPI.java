package org.apache.rocketmq.sdk.shade.common.filter;

import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;

import java.net.URL;

public class FilterAPI {
    public static URL classFile(String className) {
        return FilterAPI.class.getClassLoader().getResource(simpleClassName(className) + ".java");
    }

    public static String simpleClassName(String className) {
        String simple = className;
        int index = className.lastIndexOf(".");
        if (index >= 0) {
            simple = className.substring(index + 1);
        }
        return simple;
    }

    public static SubscriptionData buildSubscriptionData(String consumerGroup, String topic, String subString) throws Exception {
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setTopic(topic);
        subscriptionData.setSubString(subString);
        if (null == subString || subString.equals("*") || subString.length() == 0) {
            subscriptionData.setSubString("*");
        } else {
            String[] tags = subString.split("\\|\\|");
            if (tags.length > 0) {
                for (String tag : tags) {
                    if (tag.length() > 0) {
                        String trimString = tag.trim();
                        if (trimString.length() > 0) {
                            subscriptionData.getTagsSet().add(trimString);
                            subscriptionData.getCodeSet().add(Integer.valueOf(trimString.hashCode()));
                        }
                    }
                }
            } else {
                throw new Exception("subString split error");
            }
        }
        return subscriptionData;
    }

    public static SubscriptionData build(String topic, String subString, String type) throws Exception {
        if (ExpressionType.TAG.equals(type) || type == null) {
            return buildSubscriptionData(null, topic, subString);
        }
        if (subString == null || subString.length() < 1) {
            throw new IllegalArgumentException("Expression can't be null! " + type);
        }
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setTopic(topic);
        subscriptionData.setSubString(subString);
        subscriptionData.setExpressionType(type);
        return subscriptionData;
    }
}
