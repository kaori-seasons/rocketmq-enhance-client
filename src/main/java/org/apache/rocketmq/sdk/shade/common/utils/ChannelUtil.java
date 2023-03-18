package org.apache.rocketmq.sdk.shade.common.utils;

import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ChannelUtil {
    public static String getRemoteIp(Channel channel) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
        if (inetSocketAddress == null) {
            return "";
        }
        InetAddress inetAddr = inetSocketAddress.getAddress();
        return inetAddr != null ? inetAddr.getHostAddress() : inetSocketAddress.getHostName();
    }
}
