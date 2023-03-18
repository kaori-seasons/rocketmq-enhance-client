package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.ChannelEventListener;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.RemotingClient;
import org.apache.rocketmq.sdk.shade.remoting.common.Pair;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingUtil;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    protected static final long LOCK_TIMEOUT_MILLIS = 3000;
    protected final NettyClientConfig nettyClientConfig;
    protected final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroupWorker;
    protected final Lock lockChannelTables;
    protected final ConcurrentMap<String, ChannelWrapper> channelTables;
    private final Timer timer;
    private final AtomicReference<List<String>> namesrvAddrList;
    private final AtomicReference<String> namesrvAddrChoosed;
    private final AtomicInteger namesrvIndex;
    private final Lock lockNamesrvChannel;
    private final ExecutorService publicExecutor;
    private ExecutorService callbackExecutor;
    private final ChannelEventListener channelEventListener;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    protected RPCHook rpcHook;

    public NettyRemotingClient(NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null);
    }

    public NettyRemotingClient(NettyClientConfig nettyClientConfig, ChannelEventListener channelEventListener) {
        super(nettyClientConfig.getClientOnewaySemaphoreValue(), nettyClientConfig.getClientAsyncSemaphoreValue());
        this.bootstrap = new Bootstrap();
        this.lockChannelTables = new ReentrantLock();
        this.channelTables = new ConcurrentHashMap();
        this.timer = new Timer("ClientHouseKeepingService", true);
        this.namesrvAddrList = new AtomicReference<>();
        this.namesrvAddrChoosed = new AtomicReference<>();
        this.namesrvIndex = new AtomicInteger(initValueIndex());
        this.lockNamesrvChannel = new ReentrantLock();
        this.nettyClientConfig = nettyClientConfig;
        this.channelEventListener = channelEventListener;
        int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums <= 0 ? 4 : publicThreadNums, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyClientSelector_%d", Integer.valueOf(this.threadIndex.incrementAndGet())));
            }
        });
        if (nettyClientConfig.isUseTLS()) {
            try {
                this.sslContext = TlsHelper.buildSslContext(true);
                log.info("SSL enabled for client");
            } catch (IOException e) {
                log.error("Failed to create SSLContext", (Throwable) e);
            } catch (CertificateException e2) {
                log.error("Failed to create SSLContext", (Throwable) e2);
                throw new RuntimeException("Failed to create SSLContext", e2);
            }
        }
    }

    protected static int initValueIndex() {
        return Math.abs(new Random().nextInt() % 999) % 999;
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(this.nettyClientConfig.getClientWorkerThreads(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
            }
        });
        this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, false).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(this.nettyClientConfig.getConnectTimeoutMillis())).option(ChannelOption.SO_SNDBUF, Integer.valueOf(this.nettyClientConfig.getClientSocketSndBufSize())).option(ChannelOption.SO_RCVBUF, Integer.valueOf(this.nettyClientConfig.getClientSocketRcvBufSize())).handler(new ChannelInitializer<SocketChannel>() {
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (NettyRemotingClient.this.nettyClientConfig.isUseTLS()) {
                    if (null != NettyRemotingClient.this.sslContext) {
                        pipeline.addFirst(NettyRemotingClient.this.defaultEventExecutorGroup, "sslHandler", NettyRemotingClient.this.sslContext.newHandler(ch.alloc()));
                        NettyRemotingClient.log.info("Prepend SSL handler");
                    } else {
                        NettyRemotingClient.log.warn("Connections are insecure as SSLContext is null!");
                    }
                }
                pipeline.addLast(NettyRemotingClient.this.defaultEventExecutorGroup, new NettyEncoder(), new NettyDecoder(), new IdleStateHandler(0, 0, NettyRemotingClient.this.nettyClientConfig.getClientChannelMaxIdleTimeSeconds()), new NettyConnectManageHandler(), new NettyClientHandler());
            }
        });
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    NettyRemotingClient.this.scanResponseTable();
                } catch (Throwable e) {
                    NettyRemotingClient.log.error("scanResponseTable exception", e);
                }
            }
        }, LOCK_TIMEOUT_MILLIS, 1000);
        if (this.channelEventListener != null) {
            this.nettyEventExecutor.start();
        }
    }

    @Override
    public void shutdown() {
        try {
            this.timer.cancel();
            for (ChannelWrapper cw : this.channelTables.values()) {
                closeChannel(null, cw.getChannel());
            }
            this.channelTables.clear();
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingClient shutdown exception, ", (Throwable) e);
        }
        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e2) {
                log.error("NettyRemotingServer shutdown exception, ", (Throwable) e2);
            }
        }
    }

    public void closeChannel(String addr, Channel channel) {
        if (null != channel) {
            String addrRemote = null == addr ? RemotingHelper.parseChannelRemoteAddr(channel) : addr;
            try {
                if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    boolean removeItemFromTable = true;
                    ChannelWrapper prevCW = this.channelTables.get(addrRemote);
                    log.info("closeChannel: begin close the channel[{}] Found: {}", addrRemote, Boolean.valueOf(prevCW != null));
                    if (null == prevCW) {
                        log.info("closeChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    } else {
                        try {
                            if (prevCW.getChannel() != channel) {
                                log.info("closeChannel: the channel[{}] has been closed before, and has been created again, nothing to do.", addrRemote);
                                removeItemFromTable = false;
                            }
                        } catch (Exception e) {
                            log.error("closeChannel: close the channel exception", (Throwable) e);
                            this.lockChannelTables.unlock();
                        } catch (Throwable th) {
                            this.lockChannelTables.unlock();
                            throw th;
                        }
                    }
                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                    }
                    RemotingUtil.closeChannel(channel);
                    this.lockChannelTables.unlock();
                } else {
                    log.warn("closeChannel: try to lock channel table, but timeout, {}ms", Long.valueOf((long) LOCK_TIMEOUT_MILLIS));
                }
            } catch (InterruptedException e2) {
                log.error("closeChannel exception", (Throwable) e2);
            }
        }
    }

    @Override
    public void registerRPCHook(RPCHook rpcHook) {
        this.rpcHook = rpcHook;
    }

    public void closeChannel(Channel channel) {
        if (null != channel) {
            try {
                if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    boolean removeItemFromTable = true;
                    ChannelWrapper prevCW = null;
                    String addrRemote = null;
                    Iterator<Map.Entry<String, ChannelWrapper>> it = this.channelTables.entrySet().iterator();
                    while (true) {
                        try {
                            if (!it.hasNext()) {
                                break;
                            }
                            Map.Entry<String, ChannelWrapper> entry = it.next();
                            String key = entry.getKey();
                            ChannelWrapper prev = entry.getValue();
                            if (prev.getChannel() != null && prev.getChannel() == channel) {
                                prevCW = prev;
                                addrRemote = key;
                                break;
                            }
                        } catch (Exception e) {
                            log.error("closeChannel: close the channel exception", (Throwable) e);
                            this.lockChannelTables.unlock();
                        }
                    }
                    if (null == prevCW) {
                        log.info("eventCloseChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    }
                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                        RemotingUtil.closeChannel(channel);
                    }
                    this.lockChannelTables.unlock();
                } else {
                    log.warn("closeChannel: try to lock channel table, but timeout, {}ms", Long.valueOf((long) LOCK_TIMEOUT_MILLIS));
                }
            } catch (InterruptedException e2) {
                log.error("closeChannel exception", (Throwable) e2);
            }
        }
    }

    @Override
    public void updateNameServerAddressList(List<String> addrs) {
        List<String> old = this.namesrvAddrList.get();
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
                log.info("name server address updated. NEW : {} , OLD: {}", addrs, old);
                this.namesrvAddrList.set(addrs);
            }
        }
    }

    @Override
    public void updateProxyAddressList(List<String> addrs) {
    }

    @Override
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        long beginStartTime = System.currentTimeMillis();
        Channel channel = getAndCreateChannel(addr);
        if (channel == null || !channel.isActive()) {
            closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
        }
        try {
            if (this.rpcHook != null) {
                this.rpcHook.doBeforeRequest(addr, request);
            }
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                throw new RemotingTimeoutException("invokeSync call timeout");
            }
            RemotingCommand response = invokeSyncImpl(channel, request, timeoutMillis - costTime);
            if (this.rpcHook != null) {
                this.rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(channel), request, response);
            }
            return response;
        } catch (RemotingSendRequestException e) {
            log.warn("invokeSync: send request exception, so close the channel[{}]", addr);
            closeChannel(addr, channel);
            throw e;
        } catch (RemotingTimeoutException e2) {
            if (this.nettyClientConfig.isClientCloseSocketIfTimeout()) {
                closeChannel(addr, channel);
                log.warn("invokeSync: close socket because of timeout, {}ms, {}", Long.valueOf(timeoutMillis), addr);
            }
            log.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
            throw e2;
        }
    }

    protected Channel getAndCreateChannel(String addr) throws InterruptedException {
        if (null == addr) {
            return getAndCreateNameserverChannel();
        }
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw == null || !cw.isOK()) {
            return createChannel(addr);
        }
        return cw.getChannel();
    }

    private Channel getAndCreateNameserverChannel() throws InterruptedException {
        ChannelWrapper cw;
        ChannelWrapper cw2;
        String addr = this.namesrvAddrChoosed.get();
        if (!(addr == null || (cw2 = this.channelTables.get(addr)) == null || !cw2.isOK())) {
            return cw2.getChannel();
        }
        List<String> addrList = this.namesrvAddrList.get();
        try {
            if (this.lockNamesrvChannel.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    String addr2 = this.namesrvAddrChoosed.get();
                    if (addr2 == null || (cw = this.channelTables.get(addr2)) == null || !cw.isOK()) {
                        if (addrList != null && !addrList.isEmpty()) {
                            for (int i = 0; i < addrList.size(); i++) {
                                String newAddr = addrList.get(Math.abs(this.namesrvIndex.incrementAndGet()) % addrList.size());
                                this.namesrvAddrChoosed.set(newAddr);
                                log.info("new name server is chosen. OLD: {} , NEW: {}. namesrvIndex = {}", addr2, newAddr, this.namesrvIndex);
                                Channel channelNew = createChannel(newAddr);
                                if (channelNew != null) {
                                    this.lockNamesrvChannel.unlock();
                                    return channelNew;
                                }
                            }
                        }
                        this.lockNamesrvChannel.unlock();
                        return null;
                    }
                    Channel channel = cw.getChannel();
                    this.lockNamesrvChannel.unlock();
                    return channel;
                } catch (Exception e) {
                    log.error("getAndCreateNameserverChannel: create name server channel exception", (Throwable) e);
                    this.lockNamesrvChannel.unlock();
                    return null;
                }
            } else {
                log.warn("getAndCreateNameserverChannel: try to lock name server, but timeout, {}ms", Long.valueOf((long) LOCK_TIMEOUT_MILLIS));
                return null;
            }
        } catch (Throwable th) {
            this.lockNamesrvChannel.unlock();
            throw th;
        }
    }

    protected Channel createChannel(String addr) throws InterruptedException {
        boolean createNewConnection;
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            cw.getChannel().close();
            this.channelTables.remove(addr);
        }
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    cw = this.channelTables.get(addr);
                    if (cw == null) {
                        createNewConnection = true;
                    } else if (cw.isOK()) {
                        cw.getChannel().close();
                        this.channelTables.remove(addr);
                        createNewConnection = true;
                    } else if (!cw.getChannelFuture().isDone()) {
                        createNewConnection = false;
                    } else {
                        this.channelTables.remove(addr);
                        createNewConnection = true;
                    }
                    if (createNewConnection) {
                        ChannelFuture channelFuture = this.bootstrap.connect(RemotingHelper.string2SocketAddress(addr));
                        log.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
                        cw = new ChannelWrapper(channelFuture);
                        this.channelTables.put(addr, cw);
                    }
                    this.lockChannelTables.unlock();
                } catch (Exception e) {
                    log.error("createChannel: create channel exception", (Throwable) e);
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("createChannel: try to lock channel table, but timeout, {}ms", Long.valueOf((long) LOCK_TIMEOUT_MILLIS));
            }
            if (cw == null) {
                return null;
            }
            ChannelFuture channelFuture2 = cw.getChannelFuture();
            if (!channelFuture2.awaitUninterruptibly((long) this.nettyClientConfig.getConnectTimeoutMillis())) {
                log.warn("createChannel: connect remote host[{}] timeout {}ms, {}", addr, Integer.valueOf(this.nettyClientConfig.getConnectTimeoutMillis()), channelFuture2.toString());
                return null;
            } else if (cw.isOK()) {
                log.info("createChannel: connect remote host[{}] success, {}", addr, channelFuture2.toString());
                return cw.getChannel();
            } else {
                log.warn("createChannel: connect remote host[" + addr + "] failed, " + channelFuture2.toString(), channelFuture2.cause());
                return null;
            }
        } catch (Throwable th) {
            this.lockChannelTables.unlock();
            throw th;
        }
    }

    public void invokeAsync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        long beginStartTime = System.currentTimeMillis();
        Channel channel = getAndCreateChannel(addr);
        if (channel == null || !channel.isActive()) {
            closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
        }
        try {
            if (this.rpcHook != null) {
                this.rpcHook.doBeforeRequest(addr, request);
            }
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                throw new RemotingTooMuchRequestException("invokeAsync call timeout");
            }
            invokeAsyncImpl(channel, request, timeoutMillis - costTime, invokeCallback);
        } catch (RemotingSendRequestException e) {
            log.warn("invokeAsync: send request exception, so close the channel[{}]", addr);
            closeChannel(addr, channel);
            throw e;
        }
    }

    @Override
    public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        Channel channel = getAndCreateChannel(addr);
        if (channel == null || !channel.isActive()) {
            closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
        }
        try {
            if (this.rpcHook != null) {
                this.rpcHook.doBeforeRequest(addr, request);
            }
            invokeOnewayImpl(channel, request, timeoutMillis);
        } catch (RemotingSendRequestException e) {
            log.warn("invokeOneway: send request exception, so close the channel[{}]", addr);
            closeChannel(addr, channel);
            throw e;
        }
    }

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }
        this.processorTable.put(Integer.valueOf(requestCode), new Pair<>(processor, executorThis));
    }

    @Override
    public boolean isChannelWritable(String addr) {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw == null || !cw.isOK()) {
            return true;
        }
        return cw.isWritable();
    }

    @Override
    public List<String> getNameServerAddressList() {
        return this.namesrvAddrList.get();
    }

    @Override
    public ChannelEventListener getChannelEventListener() {
        return this.channelEventListener;
    }

    @Override
    public RPCHook getRPCHook() {
        return this.rpcHook;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.callbackExecutor != null ? this.callbackExecutor : this.publicExecutor;
    }

    @Override
    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    public static class ChannelWrapper {
        private final ChannelFuture channelFuture;

        public ChannelWrapper(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        public boolean isOK() {
            return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
        }

        public boolean isWritable() {
            return this.channelFuture.channel().isWritable();
        }

        public Channel getChannel() {
            return this.channelFuture.channel();
        }

        public ChannelFuture getChannelFuture() {
            return this.channelFuture;
        }
    }

    public class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        NettyClientHandler() {
            
        }

        public void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            NettyRemotingClient.this.processMessageReceived(ctx, msg);
        }
    }

    public class NettyConnectManageHandler extends ChannelDuplexHandler {
        NettyConnectManageHandler() {
            
        }

        @Override 
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            String local = localAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(localAddress);
            String remote = remoteAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(remoteAddress);
            NettyRemotingClient.log.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);
            super.connect(ctx, remoteAddress, localAddress, promise);
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remote, ctx.channel()));
            }
        }

        @Override 
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingClient.log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            NettyRemotingClient.this.closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override 
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingClient.log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            NettyRemotingClient.this.closeChannel(ctx.channel());
            super.close(ctx, promise);
            NettyRemotingClient.this.failFast(ctx.channel());
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if ((evt instanceof IdleStateEvent) && ((IdleStateEvent) evt).state().equals(IdleState.ALL_IDLE)) {
                String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                NettyRemotingClient.log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                NettyRemotingClient.this.closeChannel(ctx.channel());
                if (NettyRemotingClient.this.channelEventListener != null) {
                    NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingClient.log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            NettyRemotingClient.log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            NettyRemotingClient.this.closeChannel(ctx.channel());
            if (NettyRemotingClient.this.channelEventListener != null) {
                NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }
        }
    }

    public AtomicReference<String> getNamesrvAddrChoosed() {
        return this.namesrvAddrChoosed;
    }

    public AtomicReference<String> getProxyAddrChoosed() {
        return null;
    }
}
