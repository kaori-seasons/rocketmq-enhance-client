package org.apache.rocketmq.sdk.shade.remoting.common;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import org.springframework.beans.PropertyAccessor;

public class RemotingUtil {
    public static final String OS_NAME = System.getProperty("os.name");
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private static boolean isLinuxPlatform;
    private static boolean isWindowsPlatform;

    static {
        isLinuxPlatform = false;
        isWindowsPlatform = false;
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }

    public static Selector openSelector() throws IOException {
        SelectorProvider selectorProvider;
        Selector result = null;
        if (isLinuxPlatform()) {
            try {
                Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
                if (providerClazz != null) {
                    Method method = providerClazz.getMethod("provider", new Class[0]);
                    if (!(method == null || (selectorProvider = (SelectorProvider) method.invoke(null, new Object[0])) == null)) {
                        try {
                            result = selectorProvider.openSelector();
                        } catch (Exception e) {
                            log.warn("Open ePoll Selector for linux platform exception", (Throwable) e);
                        }
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException ignored) {
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = Selector.open();
        }
        return result;
    }

    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static String getLocalAddress() {
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> ipv4Result = new ArrayList<>();
            ArrayList<String> ipv6Result = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                Enumeration<InetAddress> en = enumeration.nextElement().getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(normalizeHostAddress(address));
                        } else {
                            ipv4Result.add(normalizeHostAddress(address));
                        }
                    }
                }
            }
            if (!ipv4Result.isEmpty()) {
                Iterator<String> it = ipv4Result.iterator();
                while (it.hasNext()) {
                    String ip = it.next();
                    if (!ip.startsWith("127.0") && !ip.startsWith("192.168")) {
                        return ip;
                    }
                }
                return ipv4Result.get(ipv4Result.size() - 1);
            } else if (!ipv6Result.isEmpty()) {
                return ipv6Result.get(0);
            } else {
                return normalizeHostAddress(InetAddress.getLocalHost());
            }
        } catch (Exception e) {
            log.error("Failed to obtain local address", (Throwable) e);
            return null;
        }
    }

    public static String normalizeHostAddress(InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return PropertyAccessor.PROPERTY_KEY_PREFIX + localHost.getHostAddress() + PropertyAccessor.PROPERTY_KEY_SUFFIX;
        }
        return localHost.getHostAddress();
    }

    public static SocketAddress string2SocketAddress(String addr) {
        String[] s = addr.split(":");
        return new InetSocketAddress(s[0], Integer.parseInt(s[1]));
    }

    public static String socketAddress2String(SocketAddress addr) {
        StringBuilder sb = new StringBuilder();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) addr;
        sb.append(inetSocketAddress.getAddress().getHostAddress());
        sb.append(":");
        sb.append(inetSocketAddress.getPort());
        return sb.toString();
    }

    public static SocketChannel connect(SocketAddress remote) {
        return connect(remote, ErrorCode.UNION);
    }

    public static SocketChannel connect(SocketAddress remote, int timeoutMillis) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);
            sc.socket().setSoLinger(false, -1);
            sc.socket().setTcpNoDelay(true);
            sc.socket().setReceiveBufferSize(65536);
            sc.socket().setSendBufferSize(65536);
            sc.socket().connect(remote, timeoutMillis);
            sc.configureBlocking(false);
            return sc;
        } catch (Exception e) {
            if (sc == null) {
                return null;
            }
            try {
                sc.close();
                return null;
            } catch (IOException e1) {
                e1.printStackTrace();
                return null;
            }
        }
    }

    public static void closeChannel(Channel channel) {
        final String addrRemote = RemotingHelper.parseChannelRemoteAddr(channel);
        channel.close().addListener((GenericFutureListener<? extends Future<? super Void>>) new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                RemotingUtil.log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote, Boolean.valueOf(future.isSuccess()));
            }
        });
    }
}
