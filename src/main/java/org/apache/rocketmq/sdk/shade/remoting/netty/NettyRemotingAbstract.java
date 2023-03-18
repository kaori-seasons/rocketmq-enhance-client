package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.ChannelEventListener;
import org.apache.rocketmq.sdk.shade.remoting.InvokeCallback;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.common.Pair;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.sdk.shade.remoting.common.ServiceThread;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class NettyRemotingAbstract {
    private static final InternalLogger log = InternalLoggerFactory.getLogger("RocketmqRemoting");
    protected final Semaphore semaphoreOneway;
    protected final Semaphore semaphoreAsync;
    protected final ConcurrentMap<Integer, ResponseFuture> responseTable = new ConcurrentHashMap(256);
    protected final HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap(64);
    protected final NettyRemotingAbstract.NettyEventExecutor nettyEventExecutor = new NettyRemotingAbstract.NettyEventExecutor();
    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;
    protected volatile SslContext sslContext;

    public NettyRemotingAbstract(int permitsOneway, int permitsAsync) {
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
    }

    public abstract ChannelEventListener getChannelEventListener();

    public void putNettyEvent(NettyEvent event) {
        this.nettyEventExecutor.putNettyEvent(event);
    }

    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        if (msg != null) {
            switch(msg.getType()) {
                case REQUEST_COMMAND:
                    this.processRequestCommand(ctx, msg);
                    break;
                case RESPONSE_COMMAND:
                    this.processResponseCommand(ctx, msg);
            }
        }

    }

    public void processRequestCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
        Pair<NettyRequestProcessor, ExecutorService> matched = (Pair)this.processorTable.get(cmd.getCode());
        final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
        final int opaque = cmd.getOpaque();
        RemotingCommand response;
        if (pair != null) {
            Runnable run = new Runnable() {
                public void run() {
                    RemotingCommand response;
                    try {
                        RPCHook rpcHook = NettyRemotingAbstract.this.getRPCHook();
                        if (rpcHook != null) {
                            rpcHook.doBeforeRequest(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd);
                        }

                        response = ((NettyRequestProcessor)pair.getObject1()).processRequest(ctx, cmd);
                        if (rpcHook != null) {
                            rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd, response);
                        }

                        if (!cmd.isOnewayRPC() && response != null) {
                            response.setOpaque(opaque);
                            response.markResponseType();

                            try {
                                ctx.writeAndFlush(response);
                            } catch (Throwable var4) {
                                NettyRemotingAbstract.log.error("process request over, but response failed", var4);
                                NettyRemotingAbstract.log.error(cmd.toString());
                                NettyRemotingAbstract.log.error(response.toString());
                            }
                        }
                    } catch (Throwable var5) {
                        NettyRemotingAbstract.log.error("process request exception", var5);
                        NettyRemotingAbstract.log.error(cmd.toString());
                        if (!cmd.isOnewayRPC()) {
                            response = RemotingCommand.createResponseCommand(1, RemotingHelper.exceptionSimpleDesc(var5));
                            response.setOpaque(opaque);
                            ctx.writeAndFlush(response);
                        }
                    }

                }
            };
            if (((NettyRequestProcessor)pair.getObject1()).rejectRequest()) {
                response = RemotingCommand.createResponseCommand(2, "[REJECTREQUEST]system busy, start flow control for a while");
                response.setOpaque(opaque);
                ctx.writeAndFlush(response);
                return;
            }

            try {
                RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
                ((ExecutorService)pair.getObject2()).submit(requestTask);
            } catch (RejectedExecutionException var9) {
                if (System.currentTimeMillis() % 10000L == 0L) {
                    log.warn(RemotingHelper.parseChannelRemoteAddr(ctx.channel()) + ", too many requests and system thread pool busy, RejectedExecutionException " + ((ExecutorService)pair.getObject2()).toString() + " request code: " + cmd.getCode());
                }

                if (!cmd.isOnewayRPC()) {
                    response = RemotingCommand.createResponseCommand(2, "[OVERLOAD]system busy, start flow control for a while");
                    response.setOpaque(opaque);
                    ctx.writeAndFlush(response);
                }
            }
        } else {
            String error = " request type " + cmd.getCode() + " not supported";
            response = RemotingCommand.createResponseCommand(3, error);
            response.setOpaque(opaque);
            ctx.writeAndFlush(response);
            log.error(RemotingHelper.parseChannelRemoteAddr(ctx.channel()) + error);
        }

    }

    public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
        int opaque = cmd.getOpaque();
        ResponseFuture responseFuture = (ResponseFuture)this.responseTable.get(opaque);
        if (responseFuture != null) {
            responseFuture.setResponseCommand(cmd);
            this.responseTable.remove(opaque);
            if (responseFuture.getInvokeCallback() != null) {
                this.executeInvokeCallback(responseFuture);
            } else {
                responseFuture.putResponse(cmd);
                responseFuture.release();
            }
        } else {
            log.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            log.warn(cmd.toString());
        }

    }

    private void executeInvokeCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(new Runnable() {
                    public void run() {
                        try {
                            responseFuture.executeInvokeCallback();
                        } catch (Throwable var5) {
                            NettyRemotingAbstract.log.warn("execute callback in executor exception, and callback throw", var5);
                        } finally {
                            responseFuture.release();
                        }

                    }
                });
            } catch (Exception var11) {
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", var11);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeInvokeCallback();
            } catch (Throwable var9) {
                log.warn("executeInvokeCallback Exception", var9);
            } finally {
                responseFuture.release();
            }
        }

    }

    public abstract RPCHook getRPCHook();

    public abstract ExecutorService getCallbackExecutor();

    public void scanResponseTable() {
        List<ResponseFuture> rfList = new LinkedList();
        Iterator it = this.responseTable.entrySet().iterator();

        ResponseFuture rf;
        while(it.hasNext()) {
            Entry<Integer, ResponseFuture> next = (Entry)it.next();
            rf = (ResponseFuture)next.getValue();
            if (rf.getBeginTimestamp() + rf.getTimeoutMillis() + 1000L <= System.currentTimeMillis()) {
                rf.release();
                it.remove();
                rfList.add(rf);
                log.warn("remove timeout request, " + rf);
            }
        }

        Iterator var7 = rfList.iterator();

        while(var7.hasNext()) {
            rf = (ResponseFuture)var7.next();

            try {
                this.executeInvokeCallback(rf);
            } catch (Throwable var6) {
                log.warn("scanResponseTable, operationComplete Exception", var6);
            }
        }

    }

    public RemotingCommand invokeSyncImpl(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        final int opaque = request.getOpaque();

        RemotingCommand var9;
        try {
            final ResponseFuture responseFuture = new ResponseFuture(channel, opaque, timeoutMillis, (InvokeCallback)null, (SemaphoreReleaseOnlyOnce)null);
            this.responseTable.put(opaque, responseFuture);
            final SocketAddress addr = channel.remoteAddress();
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        responseFuture.setSendRequestOK(true);
                    } else {
                        responseFuture.setSendRequestOK(false);
                        NettyRemotingAbstract.this.responseTable.remove(opaque);
                        responseFuture.setCause(f.cause());
                        responseFuture.putResponse((RemotingCommand)null);
                        NettyRemotingAbstract.log.warn("send a request command to channel <" + addr + "> failed.");
                    }
                }
            });
            RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis, responseFuture.getCause());
                }

                throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
            }

            var9 = responseCommand;
        } finally {
            this.responseTable.remove(opaque);
        }

        return var9;
    }

    public void invokeAsyncImpl(final Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        long beginStartTime = System.currentTimeMillis();
        final int opaque = request.getOpaque();
        boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                throw new RemotingTooMuchRequestException("invokeAsyncImpl call timeout");
            } else {
                final ResponseFuture responseFuture = new ResponseFuture(channel, opaque, timeoutMillis - costTime, invokeCallback, once);
                this.responseTable.put(opaque, responseFuture);

                try {
                    channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture f) throws Exception {
                            if (f.isSuccess()) {
                                responseFuture.setSendRequestOK(true);
                            } else {
                                NettyRemotingAbstract.this.requestFail(opaque);
                                NettyRemotingAbstract.log.warn("send a request command to channel <{}> failed.", RemotingHelper.parseChannelRemoteAddr(channel));
                            }
                        }
                    });
                } catch (Exception var15) {
                    responseFuture.release();
                    log.warn("send a request command to channel <" + RemotingHelper.parseChannelRemoteAddr(channel) + "> Exception", var15);
                    throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), var15);
                }
            }
        } else if (timeoutMillis <= 0L) {
            throw new RemotingTooMuchRequestException("invokeAsyncImpl invoke too fast");
        } else {
            String info = String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", timeoutMillis, this.semaphoreAsync.getQueueLength(), this.semaphoreAsync.availablePermits());
            log.warn(info);
            throw new RemotingTimeoutException(info);
        }
    }

    private void requestFail(int opaque) {
        ResponseFuture responseFuture = (ResponseFuture)this.responseTable.remove(opaque);
        if (responseFuture != null) {
            responseFuture.setSendRequestOK(false);
            responseFuture.putResponse((RemotingCommand)null);

            try {
                this.executeInvokeCallback(responseFuture);
            } catch (Throwable var7) {
                log.warn("execute callback in requestFail, and callback throw", var7);
            } finally {
                responseFuture.release();
            }
        }

    }

    protected void failFast(Channel channel) {
        Iterator it = this.responseTable.entrySet().iterator();

        while(it.hasNext()) {
            Entry<Integer, ResponseFuture> entry = (Entry)it.next();
            if (((ResponseFuture)entry.getValue()).getProcessChannel() == channel) {
                Integer opaque = (Integer)entry.getKey();
                if (opaque != null) {
                    this.requestFail(opaque);
                }
            }
        }

    }

    public void invokeOnewayImpl(final Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        request.markOnewayRPC();
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);

            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture f) throws Exception {
                        once.release();
                        if (!f.isSuccess()) {
                            NettyRemotingAbstract.log.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                        }

                    }
                });
            } catch (Exception var8) {
                once.release();
                log.warn("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), var8);
            }
        } else if (timeoutMillis <= 0L) {
            throw new RemotingTooMuchRequestException("invokeOnewayImpl invoke too fast");
        } else {
            String info = String.format("invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", timeoutMillis, this.semaphoreOneway.getQueueLength(), this.semaphoreOneway.availablePermits());
            log.warn(info);
            throw new RemotingTimeoutException(info);
        }
    }

    static {
        NettyLogger.initNettyLogger();
    }

    class NettyEventExecutor extends ServiceThread {
        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue();
        private final int maxSize = 10000;

        NettyEventExecutor() {
        }

        public void putNettyEvent(NettyEvent event) {
            if (this.eventQueue.size() <= 10000) {
                this.eventQueue.add(event);
            } else {
                NettyRemotingAbstract.log.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), event.toString());
            }

        }

        public void run() {
            NettyRemotingAbstract.log.info(this.getServiceName() + " service started");
            ChannelEventListener listener = NettyRemotingAbstract.this.getChannelEventListener();

            while(!this.isStopped()) {
                try {
                    NettyEvent event = (NettyEvent)this.eventQueue.poll(3000L, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        switch(event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                        }
                    }
                } catch (Exception var3) {
                    NettyRemotingAbstract.log.warn(this.getServiceName() + " service has exception. ", var3);
                }
            }

            NettyRemotingAbstract.log.info(this.getServiceName() + " service end");
        }

        public String getServiceName() {
            return NettyRemotingAbstract.NettyEventExecutor.class.getSimpleName();
        }
    }
}
