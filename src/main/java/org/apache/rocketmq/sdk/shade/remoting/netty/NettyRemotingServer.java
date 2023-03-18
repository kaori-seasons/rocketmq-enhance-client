package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.ChannelEventListener;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.RemotingServer;
import org.apache.rocketmq.sdk.shade.remoting.common.Pair;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingUtil;
import org.apache.rocketmq.sdk.shade.remoting.common.TlsMode;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupSelector;
    private final EventLoopGroup eventLoopGroupBoss;
    private final NettyServerConfig nettyServerConfig;
    private final ExecutorService publicExecutor;
    private final ChannelEventListener channelEventListener;
    private final Timer timer;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private RPCHook rpcHook;
    private int port;
    private static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";
    private static final String TLS_HANDLER_NAME = "sslHandler";
    private static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

    public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
        this(nettyServerConfig, null);
    }

    public NettyRemotingServer(final NettyServerConfig nettyServerConfig, ChannelEventListener channelEventListener) {
        super(nettyServerConfig.getServerOnewaySemaphoreValue(), nettyServerConfig.getServerAsyncSemaphoreValue());
        this.timer = new Timer("ServerHouseKeepingService", true);
        this.port = 0;
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;
        this.channelEventListener = channelEventListener;
        int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums <= 0 ? 4 : publicThreadNums, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyBoss_%d", Integer.valueOf(this.threadIndex.incrementAndGet())));
            }
        });
        if (useEpoll()) {
            this.eventLoopGroupSelector = new EpollEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal;

                {
                    this.threadTotal = nettyServerConfig.getServerSelectorThreads();
                }

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", Integer.valueOf(this.threadTotal), Integer.valueOf(this.threadIndex.incrementAndGet())));
                }
            });
        } else {
            this.eventLoopGroupSelector = new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal;

                {
                    this.threadTotal = nettyServerConfig.getServerSelectorThreads();
                }

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", Integer.valueOf(this.threadTotal), Integer.valueOf(this.threadIndex.incrementAndGet())));
                }
            });
        }
        loadSslContext();
    }

    public void loadSslContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        log.info("Server is running in TLS {} mode", tlsMode.getName());
        if (tlsMode != TlsMode.DISABLED) {
            try {
                this.sslContext = TlsHelper.buildSslContext(false);
                log.info("SSLContext created for server");
            } catch (IOException e) {
                log.error("Failed to create SSLContext for server", (Throwable) e);
            } catch (CertificateException e2) {
                log.error("Failed to create SSLContext for server", (Throwable) e2);
            }
        }
    }

    private boolean useEpoll() {
        return RemotingUtil.isLinuxPlatform() && this.nettyServerConfig.isUseEpollNativeSelector() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(this.nettyServerConfig.getServerWorkerThreads(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
            }
        });
        ServerBootstrap childHandler = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector).channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_KEEPALIVE, false).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_SNDBUF, Integer.valueOf(this.nettyServerConfig.getServerSocketSndBufSize())).childOption(ChannelOption.SO_RCVBUF, Integer.valueOf(this.nettyServerConfig.getServerSocketRcvBufSize())).localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort())).childHandler(new ChannelInitializer<SocketChannel>() {
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(NettyRemotingServer.this.defaultEventExecutorGroup, NettyRemotingServer.HANDSHAKE_HANDLER_NAME, new HandshakeHandler(TlsSystemConfig.tlsMode)).addLast(NettyRemotingServer.this.defaultEventExecutorGroup, new NettyEncoder(), new NettyDecoder(), new IdleStateHandler(0, 0, NettyRemotingServer.this.nettyServerConfig.getServerChannelMaxIdleTimeSeconds()), new NettyConnectManageHandler(), new NettyServerHandler());
            }
        });
        if (this.nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            this.port = ((InetSocketAddress) this.serverBootstrap.bind().sync().channel().localAddress()).getPort();
            if (this.channelEventListener != null) {
                this.nettyEventExecutor.start();
            }
            this.timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        NettyRemotingServer.this.scanResponseTable();
                    } catch (Throwable e) {
                        NettyRemotingServer.log.error("scanResponseTable exception", e);
                    }
                }
            }, 3000, 1000);
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (this.timer != null) {
                this.timer.cancel();
            }
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupSelector.shutdownGracefully();
            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", (Throwable) e);
        }
        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e2) {
                log.error("NettyRemotingServer shutdown exception, ", (Throwable) e2);
            }
        }
    }

    @Override
    public void registerRPCHook(RPCHook rpcHook) {
        this.rpcHook = rpcHook;
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
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor = new Pair(processor, executor);
    }

    @Override
    public int localListenPort() {
        return this.port;
    }

    @Override
    public Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int requestCode) {
        return (Pair) this.processorTable.get(Integer.valueOf(requestCode));
    }

    @Override
    public RemotingCommand invokeSync(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        return invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        invokeOnewayImpl(channel, request, timeoutMillis);
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
        return this.publicExecutor;
    }

    public class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final TlsMode tlsMode;
        private static final byte HANDSHAKE_MAGIC_CODE = 22;

        HandshakeHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            msg.markReaderIndex();
            if (msg.getByte(0) == 22) {
                switch (this.tlsMode) {
                    case DISABLED:
                        ctx.close();
                        NettyRemotingServer.log.warn("Clients intend to establish a SSL connection while this server is running in SSL disabled mode");
                        break;
                    case PERMISSIVE:
                    case ENFORCING:
                        if (null == NettyRemotingServer.this.sslContext) {
                            ctx.close();
                            NettyRemotingServer.log.error("Trying to establish a SSL connection but sslContext is null");
                            break;
                        } else {
                            ctx.pipeline().addAfter(NettyRemotingServer.this.defaultEventExecutorGroup, NettyRemotingServer.HANDSHAKE_HANDLER_NAME, NettyRemotingServer.TLS_HANDLER_NAME, NettyRemotingServer.this.sslContext.newHandler(ctx.channel().alloc())).addAfter(NettyRemotingServer.this.defaultEventExecutorGroup, NettyRemotingServer.TLS_HANDLER_NAME, NettyRemotingServer.FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                            NettyRemotingServer.log.info("Handlers prepended to channel pipeline to establish SSL connection");
                            break;
                        }
                    default:
                        NettyRemotingServer.log.warn("Unknown TLS mode");
                        break;
                }
            } else if (this.tlsMode == TlsMode.ENFORCING) {
                ctx.close();
                NettyRemotingServer.log.warn("Clients intend to establish an insecure connection while this server is running in SSL enforcing mode");
            }
            msg.resetReaderIndex();
            try {
                ctx.pipeline().remove(this);
            } catch (NoSuchElementException e) {
                NettyRemotingServer.log.error("Error while removing HandshakeHandler", (Throwable) e);
            }
            ctx.fireChannelRead(msg.retain());
        }
    }

    public class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        NettyServerHandler() {
        }

        public void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            NettyRemotingServer.this.processMessageReceived(ctx, msg);
        }
    }


    public class NettyConnectManageHandler extends ChannelDuplexHandler {
        NettyConnectManageHandler() {
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            NettyRemotingServer.log.info("NETTY SERVER PIPELINE: channelRegistered {}", RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            NettyRemotingServer.log.info("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingServer.log.info("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);
            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingServer.log.info("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);
            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if ((evt instanceof IdleStateEvent) && ((IdleStateEvent) evt).state().equals(IdleState.ALL_IDLE)) {
                String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                NettyRemotingServer.log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                RemotingUtil.closeChannel(ctx.channel());
                if (NettyRemotingServer.this.channelEventListener != null) {
                    NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemotingServer.log.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
            NettyRemotingServer.log.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);
            if (NettyRemotingServer.this.channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }
            RemotingUtil.closeChannel(ctx.channel());
        }
    }
}
