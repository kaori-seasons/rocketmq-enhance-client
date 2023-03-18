package org.apache.rocketmq.sdk.shade.common.acl.common;

public interface Decoder {
    Object decode(Object obj) throws DecoderException;
}
