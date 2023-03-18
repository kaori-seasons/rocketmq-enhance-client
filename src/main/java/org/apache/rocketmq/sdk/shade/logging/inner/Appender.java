package org.apache.rocketmq.sdk.shade.logging.inner;

import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.Vector;

public abstract class Appender {
    public static final int CODE_WRITE_FAILURE = 1;
    public static final int CODE_FLUSH_FAILURE = 2;
    public static final int CODE_CLOSE_FAILURE = 3;
    public static final int CODE_FILE_OPEN_FAILURE = 4;
    public static final String LINE_SEP = System.getProperty("line.separator");
    protected Layout layout;
    protected String name;
    boolean firstTime = true;
    protected boolean closed = false;

    public interface AppenderPipeline {
        void addAppender(Appender appender);

        Enumeration getAllAppenders();

        Appender getAppender(String str);

        boolean isAttached(Appender appender);

        void removeAllAppenders();

        void removeAppender(Appender appender);

        void removeAppender(String str);
    }

    protected abstract void append(LoggingEvent loggingEvent);

    public abstract void close();

    public void activateOptions() {
    }

    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            SysLogger.error("Finalizing appender named [" + this.name + "]. error", throwable);
        }
        if (!this.closed) {
            SysLogger.debug("Finalizing appender named [" + this.name + "].");
            close();
        }
    }

    public Layout getLayout() {
        return this.layout;
    }

    public final String getName() {
        return this.name;
    }

    public synchronized void doAppend(LoggingEvent event) {
        if (this.closed) {
            SysLogger.error("Attempted to append to closed appender named [" + this.name + "].");
        } else {
            append(event);
        }
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void handleError(String message, Exception e, int errorCode) {
        if ((e instanceof InterruptedIOException) || (e instanceof InterruptedException)) {
            Thread.currentThread().interrupt();
        }
        if (this.firstTime) {
            SysLogger.error(message + " code:" + errorCode, e);
            this.firstTime = false;
        }
    }

    public void handleError(String message) {
        if (this.firstTime) {
            SysLogger.error(message);
            this.firstTime = false;
        }
    }

    public static class AppenderPipelineImpl implements AppenderPipeline {
        protected Vector<Appender> appenderList;

        @Override
        public void addAppender(Appender newAppender) {
            if (newAppender != null) {
                if (this.appenderList == null) {
                    this.appenderList = new Vector<>(1);
                }
                if (!this.appenderList.contains(newAppender)) {
                    this.appenderList.addElement(newAppender);
                }
            }
        }

        public int appendLoopOnAppenders(LoggingEvent event) {
            int size = 0;
            if (this.appenderList != null) {
                size = this.appenderList.size();
                for (int i = 0; i < size; i++) {
                    this.appenderList.elementAt(i).doAppend(event);
                }
            }
            return size;
        }

        @Override
        public Enumeration getAllAppenders() {
            if (this.appenderList == null) {
                return null;
            }
            return this.appenderList.elements();
        }

        @Override
        public Appender getAppender(String name) {
            if (this.appenderList == null || name == null) {
                return null;
            }
            int size = this.appenderList.size();
            for (int i = 0; i < size; i++) {
                Appender appender = this.appenderList.elementAt(i);
                if (name.equals(appender.getName())) {
                    return appender;
                }
            }
            return null;
        }

        @Override
        public boolean isAttached(Appender appender) {
            if (this.appenderList == null || appender == null) {
                return false;
            }
            int size = this.appenderList.size();
            for (int i = 0; i < size; i++) {
                if (this.appenderList.elementAt(i) == appender) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void removeAllAppenders() {
            if (this.appenderList != null) {
                int len = this.appenderList.size();
                for (int i = 0; i < len; i++) {
                    this.appenderList.elementAt(i).close();
                }
                this.appenderList.removeAllElements();
                this.appenderList = null;
            }
        }

        @Override
        public void removeAppender(Appender appender) {
            if (appender != null && this.appenderList != null) {
                this.appenderList.removeElement(appender);
            }
        }

        @Override
        public void removeAppender(String name) {
            if (!(name == null || this.appenderList == null)) {
                int size = this.appenderList.size();
                for (int i = 0; i < size; i++) {
                    if (name.equals(this.appenderList.elementAt(i).getName())) {
                        this.appenderList.removeElementAt(i);
                        return;
                    }
                }
            }
        }
    }
}
