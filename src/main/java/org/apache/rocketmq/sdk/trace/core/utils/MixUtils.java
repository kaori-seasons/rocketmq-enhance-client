package org.apache.rocketmq.sdk.trace.core.utils;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import org.springframework.beans.PropertyAccessor;

public class MixUtils {
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
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static String normalizeHostAddress(InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return PropertyAccessor.PROPERTY_KEY_PREFIX + localHost.getHostAddress() + PropertyAccessor.PROPERTY_KEY_SUFFIX;
        }
        return localHost.getHostAddress();
    }

    public static String toJson(Object obj, boolean prettyFormat) {
        return RemotingSerializable.toJson(obj, prettyFormat);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return (T) RemotingSerializable.fromJson(json, classOfT);
    }

    public static String replaceNull(String ori) {
        return ori == null ? "" : ori;
    }
}
