package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.PullResult;
import org.apache.rocketmq.sdk.shade.client.consumer.PullStatus;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;

import java.util.List;

public class PullResultExt extends PullResult {
    private final long suggestWhichBrokerId;
    private byte[] messageBinary;

    public PullResultExt(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset, List<MessageExt> msgFoundList, long suggestWhichBrokerId, byte[] messageBinary) {
        super(pullStatus, nextBeginOffset, minOffset, maxOffset, msgFoundList);
        this.suggestWhichBrokerId = suggestWhichBrokerId;
        this.messageBinary = messageBinary;
    }

    public byte[] getMessageBinary() {
        return this.messageBinary;
    }

    public void setMessageBinary(byte[] messageBinary) {
        this.messageBinary = messageBinary;
    }

    public long getSuggestWhichBrokerId() {
        return this.suggestWhichBrokerId;
    }
}
