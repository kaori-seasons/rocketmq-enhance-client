package org.apache.rocketmq.sdk.shade.remoting.protocol;

public enum SerializeType {
    JSON((byte) 0),
    ROCKETMQ((byte) 1);
    
    private byte code;

    SerializeType(byte code) {
        this.code = code;
    }

    public static SerializeType valueOf(byte code) {
        SerializeType[] values = values();
        for (SerializeType serializeType : values) {
            if (serializeType.getCode() == code) {
                return serializeType;
            }
        }
        return null;
    }

    public byte getCode() {
        return this.code;
    }
}
