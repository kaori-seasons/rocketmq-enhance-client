package org.apache.rocketmq.sdk.shade.logging.inner;

public abstract class Layout {
    public abstract String format(LoggingEvent loggingEvent);

    public abstract boolean ignoresThrowable();

    public String getContentType() {
        return "text/plain";
    }

    public String getHeader() {
        return null;
    }

    public String getFooter() {
        return null;
    }
}
