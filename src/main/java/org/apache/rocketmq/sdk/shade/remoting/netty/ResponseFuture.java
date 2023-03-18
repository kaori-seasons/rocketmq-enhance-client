package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.PropertyAccessor;

public class ResponseFuture {
    private final int opaque;
    private final Channel processChannel;
    private final long timeoutMillis;
    private final InvokeCallback invokeCallback;
    private final SemaphoreReleaseOnlyOnce once;
    private volatile RemotingCommand responseCommand;
    private volatile Throwable cause;
    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);
    private volatile boolean sendRequestOK = true;

    public ResponseFuture(Channel channel, int opaque, long timeoutMillis, InvokeCallback invokeCallback, SemaphoreReleaseOnlyOnce once) {
        this.opaque = opaque;
        this.processChannel = channel;
        this.timeoutMillis = timeoutMillis;
        this.invokeCallback = invokeCallback;
        this.once = once;
    }

    public void executeInvokeCallback() {
        if (this.invokeCallback != null && this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
            this.invokeCallback.operationComplete(this);
        }
    }

    public void release() {
        if (this.once != null) {
            this.once.release();
        }
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() - this.beginTimestamp > this.timeoutMillis;
    }

    public RemotingCommand waitResponse(long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }

    public void putResponse(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return this.beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return this.sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    public InvokeCallback getInvokeCallback() {
        return this.invokeCallback;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public RemotingCommand getResponseCommand() {
        return this.responseCommand;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public int getOpaque() {
        return this.opaque;
    }

    public Channel getProcessChannel() {
        return this.processChannel;
    }

    public String toString() {
        return "ResponseFuture [responseCommand=" + this.responseCommand + ", sendRequestOK=" + this.sendRequestOK + ", cause=" + this.cause + ", opaque=" + this.opaque + ", processChannel=" + this.processChannel + ", timeoutMillis=" + this.timeoutMillis + ", invokeCallback=" + this.invokeCallback + ", beginTimestamp=" + this.beginTimestamp + ", countDownLatch=" + this.countDownLatch + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
