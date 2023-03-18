package org.apache.rocketmq.sdk.shade.common.acl.common;

public interface BinaryDecoder extends Decoder {
    byte[] decode(byte[] bArr) throws DecoderException;
}
