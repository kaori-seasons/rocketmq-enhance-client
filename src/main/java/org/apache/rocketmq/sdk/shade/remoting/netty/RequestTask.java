package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

public class RequestTask implements Runnable {
    private final Runnable runnable;
    private final Channel channel;
    private final RemotingCommand request;
    private final long createTimestamp = System.currentTimeMillis();
    private boolean stopRun = false;

    public RequestTask(Runnable runnable, Channel channel, RemotingCommand request) {
        this.runnable = runnable;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * (this.runnable != null ? this.runnable.hashCode() : 0)) + ((int) (getCreateTimestamp() ^ (getCreateTimestamp() >>> 32))))) + (this.channel != null ? this.channel.hashCode() : 0))) + (this.request != null ? this.request.hashCode() : 0))) + (isStopRun() ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RequestTask)) {
            return false;
        }
        RequestTask that = (RequestTask) o;
        if (getCreateTimestamp() != that.getCreateTimestamp() || isStopRun() != that.isStopRun()) {
            return false;
        }
        if (this.channel != null) {
            if (!this.channel.equals(that.channel)) {
                return false;
            }
        } else if (that.channel != null) {
            return false;
        }
        return this.request != null ? this.request.getOpaque() == that.request.getOpaque() : that.request == null;
    }

    public long getCreateTimestamp() {
        return this.createTimestamp;
    }

    public boolean isStopRun() {
        return this.stopRun;
    }

    public void setStopRun(boolean stopRun) {
        this.stopRun = stopRun;
    }

    @Override
    public void run() {
        if (!this.stopRun) {
            this.runnable.run();
        }
    }

    public void returnResponse(int code, String remark) {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, remark);
        response.setOpaque(this.request.getOpaque());
        this.channel.writeAndFlush(response);
    }
}
