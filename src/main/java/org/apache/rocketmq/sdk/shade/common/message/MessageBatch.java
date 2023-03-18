package org.apache.rocketmq.sdk.shade.common.message;

import org.apache.rocketmq.sdk.shade.common.MixAll;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MessageBatch extends Message implements Iterable<Message> {
    private static final long serialVersionUID = 621335151046335557L;
    private final List<Message> messages;
    static final  boolean assertResult;

    static {
        assertResult = !MessageBatch.class.desiredAssertionStatus();
    }

    private MessageBatch(List<Message> messages) {
        this.messages = messages;
    }

    public byte[] encode() {
        return MessageDecoder.encodeMessages(this.messages);
    }

    @Override
    public Iterator<Message> iterator() {
        return this.messages.iterator();
    }

    public static MessageBatch generateFromList(Collection<Message> messages) {
        if (!assertResult && messages == null) {
            throw new AssertionError();
        } else if (assertResult || messages.size() > 0) {
            List<Message> messageList = new ArrayList<>(messages.size());
            Message first = null;
            for (Message message : messages) {
                if (message.getDelayTimeLevel() > 0) {
                    throw new UnsupportedOperationException("TimeDelayLevel in not supported for batching");
                } else if (message.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    throw new UnsupportedOperationException("Retry Group is not supported for batching");
                } else {
                    if (first == null) {
                        first = message;
                    } else if (!first.getTopic().equals(message.getTopic())) {
                        throw new UnsupportedOperationException("The topic of the messages in one batch should be the same");
                    } else if (first.isWaitStoreMsgOK() != message.isWaitStoreMsgOK()) {
                        throw new UnsupportedOperationException("The waitStoreMsgOK of the messages in one batch should the same");
                    }
                    messageList.add(message);
                }
            }
            MessageBatch messageBatch = new MessageBatch(messageList);
            messageBatch.setTopic(first.getTopic());
            messageBatch.setWaitStoreMsgOK(first.isWaitStoreMsgOK());
            return messageBatch;
        } else {
            throw new AssertionError();
        }
    }
}
