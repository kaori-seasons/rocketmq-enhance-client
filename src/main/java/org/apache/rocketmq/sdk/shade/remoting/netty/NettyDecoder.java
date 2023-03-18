package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingUtil;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private static final int FRAME_MAX_LENGTH = Integer.parseInt(System.getProperty("com.rocketmq.remoting.frameMaxLength", "16777216"));

    public NettyDecoder() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            try {
                frame = (ByteBuf) super.decode(ctx, in);
                if (null == frame) {
                    if (null != frame) {
                        frame.release();
                    }
                    return null;
                }
                RemotingCommand decode = RemotingCommand.decode(frame.nioBuffer());
                if (null != frame) {
                    frame.release();
                }
                return decode;
            } catch (Exception e) {
                log.error("decode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), (Throwable) e);
                RemotingUtil.closeChannel(ctx.channel());
                if (null == frame) {
                    return null;
                }
                frame.release();
                return null;
            }
        } catch (Throwable th) {
            if (null != frame) {
                frame.release();
            }
            throw th;
        }
    }
}
