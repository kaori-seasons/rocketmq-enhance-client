package org.apache.rocketmq.sdk.shade.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLoggerFactory extends InternalLoggerFactory {
    public Slf4jLoggerFactory() {
        LoggerFactory.getILoggerFactory();
        doRegister();
    }

    @Override
    protected String getLoggerType() {
        return "slf4j";
    }

    @Override
    protected InternalLogger getLoggerInstance(String name) {
        return new Slf4jLogger(name);
    }

    @Override
    protected void shutdown() {
    }

    public static class Slf4jLogger implements InternalLogger {
        private Logger logger;

        public Slf4jLogger(String name) {
            this.logger = null;
            this.logger = LoggerFactory.getLogger(name);
        }

        @Override
        public String getName() {
            return this.logger.getName();
        }

        @Override
        public void debug(String s) {
            this.logger.debug(s);
        }

        @Override
        public void debug(String s, Object o) {
            this.logger.debug(s, o);
        }

        @Override
        public void debug(String s, Object o, Object o1) {
            this.logger.debug(s, o, o1);
        }

        @Override
        public void debug(String s, Object... objects) {
            this.logger.debug(s, objects);
        }

        @Override
        public void debug(String s, Throwable throwable) {
            this.logger.debug(s, throwable);
        }

        @Override
        public void info(String s) {
            this.logger.info(s);
        }

        @Override
        public void info(String s, Object o) {
            this.logger.info(s, o);
        }

        @Override
        public void info(String s, Object o, Object o1) {
            this.logger.info(s, o, o1);
        }

        @Override
        public void info(String s, Object... objects) {
            this.logger.info(s, objects);
        }

        @Override
        public void info(String s, Throwable throwable) {
            this.logger.info(s, throwable);
        }

        @Override
        public void warn(String s) {
            this.logger.warn(s);
        }

        @Override
        public void warn(String s, Object o) {
            this.logger.warn(s, o);
        }

        @Override
        public void warn(String s, Object... objects) {
            this.logger.warn(s, objects);
        }

        @Override
        public void warn(String s, Object o, Object o1) {
            this.logger.warn(s, o, o1);
        }

        @Override
        public void warn(String s, Throwable throwable) {
            this.logger.warn(s, throwable);
        }

        @Override
        public void error(String s) {
            this.logger.error(s);
        }

        @Override
        public void error(String s, Object o) {
            this.logger.error(s, o);
        }

        @Override
        public void error(String s, Object o, Object o1) {
            this.logger.error(s, o, o1);
        }

        @Override
        public void error(String s, Object... objects) {
            this.logger.error(s, objects);
        }

        @Override
        public void error(String s, Throwable throwable) {
            this.logger.error(s, throwable);
        }
    }
}
