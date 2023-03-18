package org.apache.rocketmq.sdk.shade.common.acl.common;

public interface Encoder {
    Object encode(Object obj) throws EncoderException;
}
