package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.shade.common.protocol.RequestCode;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.ChannelEventListener;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NettyRemotingProxyClient extends NettyRemotingClient {
    protected final ConcurrentMap<String, NettyRemotingClient.ChannelWrapper> brokerChannelTables = new ConcurrentHashMap();
    private final Random random = new Random();
    private final AtomicReference<List<String>> proxyAddrList = new AtomicReference<>();
    private final AtomicReference<String> proxyAddrChoosed = new AtomicReference<>();
    private final AtomicInteger proxyIndex = new AtomicInteger(initValueIndex());
    private final Lock lockProxyChannel = new ReentrantLock();
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    public static final Set<Integer> NAMESRV_CODE = new HashSet();
    public static final Set<Integer> BROKER_CODE = new HashSet();

    static {
        NAMESRV_CODE.add(105);
        BROKER_CODE.add(10);
        BROKER_CODE.add(Integer.valueOf((int) RequestCode.SEND_MESSAGE_V2));
        BROKER_CODE.add(36);
        BROKER_CODE.add(11);
        BROKER_CODE.add(12);
        BROKER_CODE.add(34);
        BROKER_CODE.add(35);
        BROKER_CODE.add(38);
        BROKER_CODE.add(14);
        BROKER_CODE.add(37);
        BROKER_CODE.add(41);
        BROKER_CODE.add(42);
        BROKER_CODE.add(46);
        BROKER_CODE.add(30);
    }

    public NettyRemotingProxyClient(NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig);
    }

    public NettyRemotingProxyClient(NettyClientConfig nettyClientConfig, ChannelEventListener channelEventListener) {
        super(nettyClientConfig, channelEventListener);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        if (BROKER_CODE.contains(Integer.valueOf(request.getCode()))) {
            request.addExtField(Constants.BROKER_ADDRESS, addr);
        }
        return super.invokeSync(addr, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        if (BROKER_CODE.contains(Integer.valueOf(request.getCode()))) {
            request.addExtField(Constants.BROKER_ADDRESS, addr);
        }
        super.invokeAsync(addr, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        if (BROKER_CODE.contains(Integer.valueOf(request.getCode()))) {
            request.addExtField(Constants.BROKER_ADDRESS, addr);
        }
        super.invokeOneway(addr, request, timeoutMillis);
    }

    @Override
    protected Channel getAndCreateChannel(String addr) throws InterruptedException {
        if (null == addr) {
            return getAndCreateProxyChannel();
        }
        NettyRemotingClient.ChannelWrapper cw = this.brokerChannelTables.get(addr);
        if (cw == null || !cw.isOK()) {
            return createBrokerChannel(addr);
        }
        return cw.getChannel();
    }

    protected Channel getAndCreateProxyChannel() throws InterruptedException {
        NettyRemotingClient.ChannelWrapper cw;
        NettyRemotingClient.ChannelWrapper cw2;
        String addr = this.proxyAddrChoosed.get();
        if (!(addr == null || (cw2 = (NettyRemotingClient.ChannelWrapper) this.channelTables.get(addr)) == null || !cw2.isOK())) {
            return cw2.getChannel();
        }
        List<String> addrList = this.proxyAddrList.get();
        try {
            if (this.lockProxyChannel.tryLock(3000, TimeUnit.MILLISECONDS)) {
                try {
                    String addr2 = this.proxyAddrChoosed.get();
                    if (addr2 == null || (cw = (NettyRemotingClient.ChannelWrapper) this.channelTables.get(addr2)) == null || !cw.isOK()) {
                        if (addrList != null && !addrList.isEmpty()) {
                            for (int i = 0; i < addrList.size(); i++) {
                                String newAddr = addrList.get(Math.abs(this.proxyIndex.incrementAndGet()) % addrList.size());
                                this.proxyAddrChoosed.set(newAddr);
                                log.info("new proxy server is chosen. OLD: {} , NEW: {}. proxyIndex = {}", addr2, newAddr, this.proxyIndex);
                                Channel channelNew = createChannel(newAddr);
                                if (channelNew != null) {
                                    this.lockProxyChannel.unlock();
                                    return channelNew;
                                }
                            }
                        }
                        this.lockProxyChannel.unlock();
                        return null;
                    }
                    Channel channel = cw.getChannel();
                    this.lockProxyChannel.unlock();
                    return channel;
                } catch (Exception e) {
                    log.error("getAndCreateProxyChannel: create proxy channel exception", (Throwable) e);
                    this.lockProxyChannel.unlock();
                    return null;
                }
            } else {
                log.warn("getAndCreateProxyChannel: try to lock proxy, but timeout, {}ms", (Object) 3000L);
                return null;
            }
        } catch (Throwable th) {
            this.lockProxyChannel.unlock();
            throw th;
        }
    }

    private Channel getAndCreateBrokerChannel() throws InterruptedException {
        return null;
    }

    private Channel createBrokerChannel(String addr) throws InterruptedException {
        boolean createNewConnection;
        NettyRemotingClient.ChannelWrapper cw = (NettyRemotingClient.ChannelWrapper) this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }
        this.proxyAddrList.get();
        try {
            if (this.lockProxyChannel.tryLock(3000, TimeUnit.MILLISECONDS)) {
                try {
                    cw = this.brokerChannelTables.get(addr);
                    if (cw == null) {
                        createNewConnection = true;
                    } else if (cw.isOK()) {
                        cw.getChannel().close();
                        this.brokerChannelTables.remove(addr);
                        createNewConnection = true;
                    } else if (!cw.getChannelFuture().isDone()) {
                        createNewConnection = false;
                    } else {
                        this.brokerChannelTables.remove(addr);
                        createNewConnection = true;
                    }
                    if (createNewConnection) {
                        ChannelFuture channelFuture = this.bootstrap.connect(RemotingHelper.string2SocketAddress(getRandomProxyAddr()));
                        log.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
                        cw = new NettyRemotingClient.ChannelWrapper(channelFuture);
                        this.brokerChannelTables.put(addr, cw);
                    }
                    this.lockProxyChannel.unlock();
                } catch (Exception e) {
                    log.error("createBrokerChannel: create broker channel exception", (Throwable) e);
                    this.lockProxyChannel.unlock();
                }
            } else {
                log.warn("createBrokerChannel: try to lock broker server, but timeout, {}ms", (Object) 3000L);
            }
            if (cw == null) {
                return null;
            }
            ChannelFuture channelFuture2 = cw.getChannelFuture();
            if (!channelFuture2.awaitUninterruptibly((long) this.nettyClientConfig.getConnectTimeoutMillis())) {
                log.warn("createBrokerChannel: connect remote host[{}] timeout {}ms, {}", addr, Integer.valueOf(this.nettyClientConfig.getConnectTimeoutMillis()), channelFuture2.toString());
                return null;
            } else if (cw.isOK()) {
                log.info("createBrokerChannel: connect remote host[{}] success, {}", addr, channelFuture2.toString());
                return cw.getChannel();
            } else {
                log.warn("createBrokerChannel: connect remote host[" + addr + "] failed, " + channelFuture2.toString(), channelFuture2.cause());
                return null;
            }
        } catch (Throwable th) {
            this.lockProxyChannel.unlock();
            throw th;
        }
    }

    @Override
    public void updateProxyAddressList(List<String> addrs) {
        List<String> old = this.proxyAddrList.get();
        boolean update = false;
        if (!addrs.isEmpty()) {
            if (null == old) {
                update = true;
            } else if (addrs.size() != old.size()) {
                update = true;
            } else {
                for (int i = 0; i < addrs.size() && !update; i++) {
                    if (!old.contains(addrs.get(i))) {
                        update = true;
                    }
                }
            }
            if (update) {
                Collections.shuffle(addrs);
                log.info("proxy address updated. NEW : {} , OLD: {}", addrs, old);
                this.proxyAddrList.set(addrs);
            }
        }
    }

    private String getRandomProxyAddr() {
        List<String> addrList = this.proxyAddrList.get();
        return addrList.get(this.random.nextInt(100) % addrList.size());
    }

    @Override
    public AtomicReference<String> getProxyAddrChoosed() {
        return this.proxyAddrChoosed;
    }
}
