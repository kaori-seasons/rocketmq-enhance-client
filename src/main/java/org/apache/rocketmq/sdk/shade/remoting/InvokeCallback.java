package org.apache.rocketmq.sdk.shade.remoting;

import org.apache.rocketmq.sdk.shade.remoting.netty.ResponseFuture;

public interface InvokeCallback {
    void operationComplete(ResponseFuture responseFuture);
}
