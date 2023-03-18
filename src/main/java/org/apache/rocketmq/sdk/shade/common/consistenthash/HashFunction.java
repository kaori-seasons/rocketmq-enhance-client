package org.apache.rocketmq.sdk.shade.common.consistenthash;

public interface HashFunction {
    long hash(String str);
}
