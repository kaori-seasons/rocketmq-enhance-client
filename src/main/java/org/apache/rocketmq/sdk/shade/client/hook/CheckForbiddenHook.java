package org.apache.rocketmq.sdk.shade.client.hook;

import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;

public interface CheckForbiddenHook {
    String hookName();

    void checkForbidden(CheckForbiddenContext checkForbiddenContext) throws MQClientException;
}
