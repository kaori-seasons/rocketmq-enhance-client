package org.apache.rocketmq.sdk.shade.common.filter;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

public interface MessageFilter {
    boolean match(MessageExt messageExt, FilterContext filterContext);
}
