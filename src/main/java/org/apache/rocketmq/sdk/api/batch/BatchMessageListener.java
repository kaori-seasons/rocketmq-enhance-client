package org.apache.rocketmq.sdk.api.batch;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Message;
import java.util.List;

public interface BatchMessageListener {
    Action consume(List<Message> list, ConsumeContext consumeContext);
}
