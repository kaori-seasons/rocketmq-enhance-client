package org.apache.rocketmq.sdk.shade.remoting.netty;

import io.netty.channel.Channel;
import org.springframework.beans.PropertyAccessor;

public class NettyEvent {
    private final NettyEventType type;
    private final String remoteAddr;
    private final Channel channel;

    public NettyEvent(NettyEventType type, String remoteAddr, Channel channel) {
        this.type = type;
        this.remoteAddr = remoteAddr;
        this.channel = channel;
    }

    public NettyEventType getType() {
        return this.type;
    }

    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public String toString() {
        return "NettyEvent [type=" + this.type + ", remoteAddr=" + this.remoteAddr + ", channel=" + this.channel + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
