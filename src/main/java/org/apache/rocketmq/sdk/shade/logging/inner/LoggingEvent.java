package org.apache.rocketmq.sdk.shade.logging.inner;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

public class LoggingEvent implements Serializable {
    public final transient String fqnOfCategoryClass;
    private transient Object message;
    private transient Level level;
    private transient Logger logger;
    private String renderedMessage;
    private String threadName;
    public final long timeStamp = System.currentTimeMillis();
    private Throwable throwable;

    public LoggingEvent(String fqnOfCategoryClass, Logger logger, Level level, Object message, Throwable throwable) {
        this.fqnOfCategoryClass = fqnOfCategoryClass;
        this.message = message;
        this.logger = logger;
        this.throwable = throwable;
        this.level = level;
    }

    public Object getMessage() {
        if (this.message != null) {
            return this.message;
        }
        return getRenderedMessage();
    }

    public String getRenderedMessage() {
        if (this.renderedMessage == null && this.message != null) {
            if (this.message instanceof String) {
                this.renderedMessage = (String) this.message;
            } else {
                this.renderedMessage = this.message.toString();
            }
        }
        return this.renderedMessage;
    }

    public String getThreadName() {
        if (this.threadName == null) {
            this.threadName = Thread.currentThread().getName();
        }
        return this.threadName;
    }

    public Level getLevel() {
        return this.level;
    }

    public String getLoggerName() {
        return this.logger.getName();
    }

    public String[] getThrowableStr() {
        if (this.throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            this.throwable.printStackTrace(pw);
        } catch (RuntimeException ex) {
            SysLogger.warn("InnerLogger print stack trace error", ex);
        }
        pw.flush();
        LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        ArrayList<String> lines = new ArrayList<>();
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
        } catch (IOException ex2) {
            if (ex2 instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex2.toString());
        }
        String[] tempRep = new String[lines.size()];
        lines.toArray(tempRep);
        return tempRep;
    }
}
