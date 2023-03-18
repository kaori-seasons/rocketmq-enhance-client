package org.apache.rocketmq.sdk.shade.remoting.netty;

import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyLogger {
    private static AtomicBoolean nettyLoggerSeted = new AtomicBoolean(false);
    private static InternalLogLevel nettyLogLevel = InternalLogLevel.ERROR;

    public static void initNettyLogger() {
        if (!nettyLoggerSeted.get()) {
            try {
                InternalLoggerFactory.setDefaultFactory(new NettyBridgeLoggerFactory());
            } catch (Throwable th) {
            }
            nettyLoggerSeted.set(true);
        }
    }

    private static class NettyBridgeLogger implements InternalLogger {
        private org.apache.rocketmq.sdk.shade.logging.InternalLogger logger = null;

        public NettyBridgeLogger(String name) {
            this.logger = org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory.getLogger(name);
        }

        @Override
        public String name() {
            return this.logger.getName();
        }

        @Override
        public boolean isEnabled(InternalLogLevel internalLogLevel) {
            return NettyLogger.nettyLogLevel.ordinal() <= internalLogLevel.ordinal();
        }

        @Override
        public void log(InternalLogLevel internalLogLevel, String s) {
            if (internalLogLevel.equals(InternalLogLevel.DEBUG)) {
                this.logger.debug(s);
            }

            if (internalLogLevel.equals(InternalLogLevel.TRACE)) {
                this.logger.info(s);
            }

            if (internalLogLevel.equals(InternalLogLevel.INFO)) {
                this.logger.info(s);
            }

            if (internalLogLevel.equals(InternalLogLevel.WARN)) {
                this.logger.warn(s);
            }

            if (internalLogLevel.equals(InternalLogLevel.ERROR)) {
                this.logger.error(s);
            }

        }

        public void log(InternalLogLevel internalLogLevel, String s, Object o) {
            if (internalLogLevel.equals(InternalLogLevel.DEBUG)) {
                this.logger.debug(s, o);
            }

            if (internalLogLevel.equals(InternalLogLevel.TRACE)) {
                this.logger.info(s, o);
            }

            if (internalLogLevel.equals(InternalLogLevel.INFO)) {
                this.logger.info(s, o);
            }

            if (internalLogLevel.equals(InternalLogLevel.WARN)) {
                this.logger.warn(s, o);
            }

            if (internalLogLevel.equals(InternalLogLevel.ERROR)) {
                this.logger.error(s, o);
            }

        }

        public void log(InternalLogLevel internalLogLevel, String s, Object o, Object o1) {
            if (internalLogLevel.equals(InternalLogLevel.DEBUG)) {
                this.logger.debug(s, o, o1);
            }

            if (internalLogLevel.equals(InternalLogLevel.TRACE)) {
                this.logger.info(s, o, o1);
            }

            if (internalLogLevel.equals(InternalLogLevel.INFO)) {
                this.logger.info(s, o, o1);
            }

            if (internalLogLevel.equals(InternalLogLevel.WARN)) {
                this.logger.warn(s, o, o1);
            }

            if (internalLogLevel.equals(InternalLogLevel.ERROR)) {
                this.logger.error(s, o, o1);
            }

        }

        public void log(InternalLogLevel internalLogLevel, String s, Object... objects) {
            if (internalLogLevel.equals(InternalLogLevel.DEBUG)) {
                this.logger.debug(s, objects);
            }

            if (internalLogLevel.equals(InternalLogLevel.TRACE)) {
                this.logger.info(s, objects);
            }

            if (internalLogLevel.equals(InternalLogLevel.INFO)) {
                this.logger.info(s, objects);
            }

            if (internalLogLevel.equals(InternalLogLevel.WARN)) {
                this.logger.warn(s, objects);
            }

            if (internalLogLevel.equals(InternalLogLevel.ERROR)) {
                this.logger.error(s, objects);
            }

        }

        public void log(InternalLogLevel internalLogLevel, String s, Throwable throwable) {
            if (internalLogLevel.equals(InternalLogLevel.DEBUG)) {
                this.logger.debug(s, throwable);
            }

            if (internalLogLevel.equals(InternalLogLevel.TRACE)) {
                this.logger.info(s, throwable);
            }

            if (internalLogLevel.equals(InternalLogLevel.INFO)) {
                this.logger.info(s, throwable);
            }

            if (internalLogLevel.equals(InternalLogLevel.WARN)) {
                this.logger.warn(s, throwable);
            }

            if (internalLogLevel.equals(InternalLogLevel.ERROR)) {
                this.logger.error(s, throwable);
            }

        }

        @Override
        public void log(InternalLogLevel internalLogLevel, Throwable throwable) {

        }

        public boolean isTraceEnabled() {
            return this.isEnabled(InternalLogLevel.TRACE);
        }

        public void trace(String var1) {
            this.logger.info(var1);
        }

        public void trace(String var1, Object var2) {
            this.logger.info(var1, var2);
        }

        public void trace(String var1, Object var2, Object var3) {
            this.logger.info(var1, var2, var3);
        }

        public void trace(String var1, Object... var2) {
            this.logger.info(var1, var2);
        }

        public void trace(String var1, Throwable var2) {
            this.logger.info(var1, var2);
        }

        @Override
        public void trace(Throwable throwable) {

        }

        public boolean isDebugEnabled() {
            return this.isEnabled(InternalLogLevel.DEBUG);
        }

        public void debug(String var1) {
            this.logger.debug(var1);
        }

        public void debug(String var1, Object var2) {
            this.logger.debug(var1, var2);
        }

        public void debug(String var1, Object var2, Object var3) {
            this.logger.debug(var1, var2, var3);
        }

        public void debug(String var1, Object... var2) {
            this.logger.debug(var1, var2);
        }

        public void debug(String var1, Throwable var2) {
            this.logger.debug(var1, var2);
        }

        @Override
        public void debug(Throwable throwable) {

        }

        public boolean isInfoEnabled() {
            return this.isEnabled(InternalLogLevel.INFO);
        }

        public void info(String var1) {
            this.logger.info(var1);
        }

        public void info(String var1, Object var2) {
            this.logger.info(var1, var2);
        }

        public void info(String var1, Object var2, Object var3) {
            this.logger.info(var1, var2, var3);
        }

        public void info(String var1, Object... var2) {
            this.logger.info(var1, var2);
        }

        public void info(String var1, Throwable var2) {
            this.logger.info(var1, var2);
        }

        @Override
        public void info(Throwable throwable) {

        }

        public boolean isWarnEnabled() {
            return this.isEnabled(InternalLogLevel.WARN);
        }

        public void warn(String var1) {
            this.logger.warn(var1);
        }

        public void warn(String var1, Object var2) {
            this.logger.warn(var1, var2);
        }

        public void warn(String var1, Object... var2) {
            this.logger.warn(var1, var2);
        }

        public void warn(String var1, Object var2, Object var3) {
            this.logger.warn(var1, var2, var3);
        }

        public void warn(String var1, Throwable var2) {
            this.logger.warn(var1, var2);
        }

        @Override
        public void warn(Throwable throwable) {

        }

        public boolean isErrorEnabled() {
            return this.isEnabled(InternalLogLevel.ERROR);
        }

        public void error(String var1) {
            this.logger.error(var1);
        }

        public void error(String var1, Object var2) {
            this.logger.error(var1, var2);
        }

        public void error(String var1, Object var2, Object var3) {
            this.logger.error(var1, var2, var3);
        }

        public void error(String var1, Object... var2) {
            this.logger.error(var1, var2);
        }

        public void error(String var1, Throwable var2) {
            this.logger.error(var1, var2);
        }

        @Override
        public void error(Throwable throwable) {

        }
    }

    private static class NettyBridgeLoggerFactory extends InternalLoggerFactory {
        private NettyBridgeLoggerFactory() {
        }

        protected InternalLogger newInstance(String s) {
            return new NettyLogger.NettyBridgeLogger(s);
        }
    }
}
