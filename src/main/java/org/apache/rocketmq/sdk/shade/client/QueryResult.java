package org.apache.rocketmq.sdk.shade.client;

import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import java.util.List;

import org.springframework.beans.PropertyAccessor;

public class QueryResult {
    private final long indexLastUpdateTimestamp;
    private final List<MessageExt> messageList;

    public QueryResult(long indexLastUpdateTimestamp, List<MessageExt> messageList) {
        this.indexLastUpdateTimestamp = indexLastUpdateTimestamp;
        this.messageList = messageList;
    }

    public long getIndexLastUpdateTimestamp() {
        return this.indexLastUpdateTimestamp;
    }

    public List<MessageExt> getMessageList() {
        return this.messageList;
    }

    public String toString() {
        return "QueryResult [indexLastUpdateTimestamp=" + this.indexLastUpdateTimestamp + ", messageList=" + this.messageList + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
