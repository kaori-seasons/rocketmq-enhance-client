package org.apache.rocketmq.sdk.shade.common.protocol;

import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import org.apache.commons.lang3.StringUtils;

public class NamespaceUtil {
    public static final char NAMESPACE_SEPARATOR = '%';
    public static final String STRING_BLANK = "";
    public static final int RETRY_PREFIX_LENGTH = MixAll.RETRY_GROUP_TOPIC_PREFIX.length();
    public static final int DLQ_PREFIX_LENGTH = MixAll.DLQ_GROUP_TOPIC_PREFIX.length();

    public static String withNamespace(RemotingCommand request, String resource) {
        return wrapNamespace(getNamespaceFromRequest(request), resource);
    }

    public static String withoutNamespace(String resource) {
        if (StringUtils.isEmpty(resource) || isSystemResource(resource)) {
            return resource;
        }
        if (isRetryTopic(resource)) {
            int index = resource.indexOf(37, RETRY_PREFIX_LENGTH);
            if (index > 0) {
                return MixAll.getRetryTopic(resource.substring(index + 1));
            }
            return resource;
        } else if (isDLQTopic(resource)) {
            int index2 = resource.indexOf(37, DLQ_PREFIX_LENGTH);
            if (index2 > 0) {
                return MixAll.getDLQTopic(resource.substring(index2 + 1));
            }
            return resource;
        } else {
            int index3 = resource.indexOf(37);
            if (index3 > 0) {
                return resource.substring(index3 + 1);
            }
            return resource;
        }
    }

    public static String withoutNamespace(String resource, String namespace) {
        if (StringUtils.isEmpty(resource) || StringUtils.isEmpty(namespace)) {
            return resource;
        }
        StringBuffer prefixBuffer = new StringBuffer();
        if (isRetryTopic(resource)) {
            prefixBuffer.append(MixAll.RETRY_GROUP_TOPIC_PREFIX);
        } else if (isDLQTopic(resource)) {
            prefixBuffer.append(MixAll.DLQ_GROUP_TOPIC_PREFIX);
        }
        prefixBuffer.append(namespace).append('%');
        if (resource.startsWith(prefixBuffer.toString())) {
            return withoutNamespace(resource);
        }
        return resource;
    }

    public static String wrapNamespace(String namespace, String resource) {
        if (StringUtils.isEmpty(namespace) || StringUtils.isEmpty(resource)) {
            return resource;
        }
        if (isSystemResource(resource)) {
            return resource;
        }
        if (isAlreadyWithNamespace(resource, namespace)) {
            return resource;
        }
        StringBuffer strBuffer = new StringBuffer().append(namespace).append('%');
        if (isRetryTopic(resource)) {
            strBuffer.append(resource.substring(RETRY_PREFIX_LENGTH));
            return strBuffer.insert(0, MixAll.RETRY_GROUP_TOPIC_PREFIX).toString();
        } else if (!isDLQTopic(resource)) {
            return strBuffer.append(resource).toString();
        } else {
            strBuffer.append(resource.substring(DLQ_PREFIX_LENGTH));
            return strBuffer.insert(0, MixAll.DLQ_GROUP_TOPIC_PREFIX).toString();
        }
    }

    public static boolean isAlreadyWithNamespace(String resource, String namespace) {
        if (StringUtils.isEmpty(namespace) || StringUtils.isEmpty(resource) || isSystemResource(resource)) {
            return false;
        }
        if (isRetryTopic(resource)) {
            resource = resource.substring(RETRY_PREFIX_LENGTH);
        }
        if (isDLQTopic(resource)) {
            resource = resource.substring(DLQ_PREFIX_LENGTH);
        }
        return resource.startsWith(namespace + '%');
    }

    public static String withNamespaceAndRetry(RemotingCommand request, String consumerGroup) {
        return wrapNamespaceAndRetry(getNamespaceFromRequest(request), consumerGroup);
    }

    public static String wrapNamespaceAndRetry(String namespace, String consumerGroup) {
        if (StringUtils.isEmpty(consumerGroup)) {
            return null;
        }
        return new StringBuffer().append(MixAll.RETRY_GROUP_TOPIC_PREFIX).append(wrapNamespace(namespace, consumerGroup)).toString();
    }

    public static String getNamespaceFromRequest(RemotingCommand request) {
        String namespace;
        if (null == request || null == request.getExtFields()) {
            return null;
        }
        switch (request.getCode()) {
            case RequestCode.SEND_MESSAGE_V2:
                namespace = request.getExtFields().get("n");
                break;
            default:
                namespace = request.getExtFields().get("namespace");
                break;
        }
        return namespace;
    }

    public static String getNamespaceFromResource(String resource) {
        if (StringUtils.isEmpty(resource) || isSystemResource(resource)) {
            return "";
        }
        if (isRetryTopic(resource)) {
            int index = resource.indexOf(37, RETRY_PREFIX_LENGTH);
            if (index > 0) {
                return resource.substring(RETRY_PREFIX_LENGTH, index);
            }
            return "";
        } else if (isDLQTopic(resource)) {
            int index2 = resource.indexOf(37, DLQ_PREFIX_LENGTH);
            if (index2 > 0) {
                return resource.substring(DLQ_PREFIX_LENGTH, index2);
            }
            return "";
        } else {
            int index3 = resource.indexOf(37);
            if (index3 > 0) {
                return resource.substring(0, index3);
            }
            return "";
        }
    }

    private static boolean isSystemResource(String resource) {
        if (StringUtils.isEmpty(resource)) {
            return false;
        }
        if (!MixAll.isSystemTopic(resource) && !MixAll.isSysConsumerGroup(resource)) {
            return MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC.equals(resource);
        }
        return true;
    }

    public static boolean isRetryTopic(String resource) {
        if (StringUtils.isEmpty(resource)) {
            return false;
        }
        return resource.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX);
    }

    public static boolean isDLQTopic(String resource) {
        if (StringUtils.isEmpty(resource)) {
            return false;
        }
        return resource.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX);
    }
}
