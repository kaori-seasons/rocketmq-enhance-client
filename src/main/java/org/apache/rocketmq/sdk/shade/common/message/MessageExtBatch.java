package org.apache.rocketmq.sdk.shade.common.message;

import java.nio.ByteBuffer;

public class MessageExtBatch extends MessageExt {
    private static final long serialVersionUID = -2353110995348498537L;
    private ByteBuffer encodedBuff;
    static final boolean assertResult;

    static {
        assertResult = !MessageExtBatch.class.desiredAssertionStatus();
    }

    public ByteBuffer wrap() {
        if (assertResult || getBody() != null) {
            return ByteBuffer.wrap(getBody(), 0, getBody().length);
        }
        throw new AssertionError();
    }

    public ByteBuffer getEncodedBuff() {
        return this.encodedBuff;
    }

    public void setEncodedBuff(ByteBuffer encodedBuff) {
        this.encodedBuff = encodedBuff;
    }
}
