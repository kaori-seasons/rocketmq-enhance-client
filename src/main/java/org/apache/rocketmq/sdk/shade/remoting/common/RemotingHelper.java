package org.apache.rocketmq.sdk.shade.remoting.common;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.springframework.util.AntPathMatcher;

public class RemotingHelper {
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String ROCKETMQ_REMOTING = "RocketmqRemoting";
    private static final InternalLogger log = InternalLoggerFactory.getLogger(ROCKETMQ_REMOTING);

    public static String exceptionSimpleDesc(Throwable e) {
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement elment = stackTrace[0];
                sb.append(", ");
                sb.append(elment.toString());
            }
        }
        return sb.toString();
    }

    public static SocketAddress string2SocketAddress(String addr) {
        int split = addr.lastIndexOf(":");
        return new InetSocketAddress(addr.substring(0, split), Integer.parseInt(addr.substring(split + 1)));
    }

    public static RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        long beginTime = System.currentTimeMillis();
        SocketChannel socketChannel = RemotingUtil.connect(RemotingUtil.string2SocketAddress(addr));
        if (socketChannel != null) {
            try {
                try {
                    socketChannel.configureBlocking(true);
                    socketChannel.socket().setSoTimeout((int) timeoutMillis);
                    ByteBuffer byteBufferRequest = request.encode();
                    while (byteBufferRequest.hasRemaining()) {
                        if (socketChannel.write(byteBufferRequest) <= 0) {
                            throw new RemotingSendRequestException(addr);
                        } else if (!byteBufferRequest.hasRemaining() || System.currentTimeMillis() - beginTime <= timeoutMillis) {
                            Thread.sleep(1);
                        } else {
                            throw new RemotingSendRequestException(addr);
                        }
                    }
                    ByteBuffer byteBufferSize = ByteBuffer.allocate(4);
                    while (byteBufferSize.hasRemaining()) {
                        if (socketChannel.read(byteBufferSize) <= 0) {
                            throw new RemotingTimeoutException(addr, timeoutMillis);
                        } else if (!byteBufferSize.hasRemaining() || System.currentTimeMillis() - beginTime <= timeoutMillis) {
                            Thread.sleep(1);
                        } else {
                            throw new RemotingTimeoutException(addr, timeoutMillis);
                        }
                    }
                    ByteBuffer byteBufferBody = ByteBuffer.allocate(byteBufferSize.getInt(0));
                    while (byteBufferBody.hasRemaining()) {
                        if (socketChannel.read(byteBufferBody) <= 0) {
                            throw new RemotingTimeoutException(addr, timeoutMillis);
                        } else if (!byteBufferBody.hasRemaining() || System.currentTimeMillis() - beginTime <= timeoutMillis) {
                            Thread.sleep(1);
                        } else {
                            throw new RemotingTimeoutException(addr, timeoutMillis);
                        }
                    }
                    byteBufferBody.flip();
                    return RemotingCommand.decode(byteBufferBody);
                } catch (IOException e) {
                    log.error("invokeSync failure", (Throwable) e);
                    if (0 != 0) {
                        throw new RemotingTimeoutException(addr, timeoutMillis);
                    }
                    throw new RemotingSendRequestException(addr);
                }
            } finally {
                try {
                    socketChannel.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        } else {
            throw new RemotingConnectException(addr);
        }
    }

    public static String parseChannelRemoteAddr(Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        String addr = remote != null ? remote.toString() : "";
        if (addr.length() <= 0) {
            return "";
        }
        int index = addr.lastIndexOf(AntPathMatcher.DEFAULT_PATH_SEPARATOR);
        if (index >= 0) {
            return addr.substring(index + 1);
        }
        return addr;
    }

    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return "";
        }
        String addr = socketAddress.toString();
        if (addr.length() > 0) {
            return addr.substring(1);
        }
        return "";
    }
}
