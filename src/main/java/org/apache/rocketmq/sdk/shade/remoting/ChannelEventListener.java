package org.apache.rocketmq.sdk.shade.remoting;

import io.netty.channel.Channel;

public interface ChannelEventListener {
    void onChannelConnect(String str, Channel channel);

    void onChannelClose(String str, Channel channel);

    void onChannelException(String str, Channel channel);

    void onChannelIdle(String str, Channel channel);
}
