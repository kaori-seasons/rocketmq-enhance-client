package org.apache.rocketmq.sdk.trace.core.dispatch;

public abstract class AsyncAppender {
    public abstract void append(Object obj);

    public abstract void flush();
}
