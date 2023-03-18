package org.apache.rocketmq.sdk.shade.common.acl.common;

public interface BinaryEncoder extends Encoder {
    byte[] encode(byte[] bArr) throws EncoderException;
}
