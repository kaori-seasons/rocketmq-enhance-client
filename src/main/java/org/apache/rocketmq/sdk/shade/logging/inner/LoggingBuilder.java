package org.apache.rocketmq.sdk.shade.logging.inner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class LoggingBuilder {
    public static final String SYSTEM_OUT = "System.out";
    public static final String SYSTEM_ERR = "System.err";
    public static final String LOGGING_ENCODING = "rocketmq.logging.inner.encoding";
    public static final String ENCODING = System.getProperty("rocketmq.logging.inner.encoding", "UTF-8");

    public LoggingBuilder() {
    }

    public static LoggingBuilder.AppenderBuilder newAppenderBuilder() {
        return new LoggingBuilder.AppenderBuilder();
    }

    public static LoggingBuilder.LayoutBuilder newLayoutBuilder() {
        return new LoggingBuilder.LayoutBuilder();
    }

    public static class DefaultLayout extends Layout {
        public DefaultLayout() {
        }

        public String format(LoggingEvent event) {
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,sss");
            String format = simpleDateFormat.format(new Date(event.timeStamp));
            sb.append(format);
            sb.append(" ");
            sb.append(event.getLevel());
            sb.append(" ");
            sb.append(event.getLoggerName());
            sb.append(" - ");
            sb.append(event.getMessage());
            String[] throwableStr = event.getThrowableStr();
            if (throwableStr != null) {
                sb.append("\r\n");
                String[] var6 = throwableStr;
                int var7 = throwableStr.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    String s = var6[var8];
                    sb.append(s);
                    sb.append("\r\n");
                }
            }

            sb.append("\r\n");
            return sb.toString();
        }

        public boolean ignoresThrowable() {
            return false;
        }
    }

    public static class SimpleLayout extends Layout {
        public SimpleLayout() {
        }

        public String format(LoggingEvent event) {
            StringBuilder sb = new StringBuilder();
            sb.append(event.getLevel().toString());
            sb.append(" - ");
            sb.append(event.getRenderedMessage());
            sb.append("\r\n");
            return sb.toString();
        }

        public boolean ignoresThrowable() {
            return false;
        }
    }

    public static class LayoutBuilder {
        private Layout layout;

        public LayoutBuilder() {
        }

        public LoggingBuilder.LayoutBuilder withSimpleLayout() {
            this.layout = new LoggingBuilder.SimpleLayout();
            return this;
        }

        public LoggingBuilder.LayoutBuilder withDefaultLayout() {
            this.layout = new LoggingBuilder.DefaultLayout();
            return this;
        }

        public Layout build() {
            if (this.layout == null) {
                this.layout = new LoggingBuilder.SimpleLayout();
            }

            return this.layout;
        }
    }

    public static class ConsoleAppender extends LoggingBuilder.WriterAppender {
        protected String target = "System.out";

        public ConsoleAppender() {
        }

        public void setTarget(String value) {
            String v = value.trim();
            if ("System.out".equalsIgnoreCase(v)) {
                this.target = "System.out";
            } else if ("System.err".equalsIgnoreCase(v)) {
                this.target = "System.err";
            } else {
                this.targetWarn(value);
            }

        }

        public String getTarget() {
            return this.target;
        }

        void targetWarn(String val) {
            SysLogger.warn("[" + val + "] should be System.out or System.err.");
            SysLogger.warn("Using previously set target, System.out by default.");
        }

        public void activateOptions() {
            if (this.target.equals("System.err")) {
                this.setWriter(this.createWriter(System.err));
            } else {
                this.setWriter(this.createWriter(System.out));
            }

            super.activateOptions();
        }

        protected final void closeWriter() {
        }
    }

    private static class RollingCalendar extends GregorianCalendar {
        private static final long serialVersionUID = -3560331770601814177L;
        int type = -1;

        RollingCalendar() {
        }

        RollingCalendar(TimeZone tz, Locale locale) {
            super(tz, locale);
        }

        void setType(int type) {
            this.type = type;
        }

        public long getNextCheckMillis(Date now) {
            return this.getNextCheckDate(now).getTime();
        }

        public Date getNextCheckDate(Date now) {
            this.setTime(now);
            switch(this.type) {
                case 0:
                    this.set(13, 0);
                    this.set(14, 0);
                    this.add(12, 1);
                    break;
                case 1:
                    this.set(12, 0);
                    this.set(13, 0);
                    this.set(14, 0);
                    this.add(11, 1);
                    break;
                case 2:
                    this.set(12, 0);
                    this.set(13, 0);
                    this.set(14, 0);
                    int hour = this.get(11);
                    if (hour < 12) {
                        this.set(11, 12);
                    } else {
                        this.set(11, 0);
                        this.add(5, 1);
                    }
                    break;
                case 3:
                    this.set(11, 0);
                    this.set(12, 0);
                    this.set(13, 0);
                    this.set(14, 0);
                    this.add(5, 1);
                    break;
                case 4:
                    this.set(7, this.getFirstDayOfWeek());
                    this.set(11, 0);
                    this.set(12, 0);
                    this.set(13, 0);
                    this.set(14, 0);
                    this.add(3, 1);
                    break;
                case 5:
                    this.set(5, 1);
                    this.set(11, 0);
                    this.set(12, 0);
                    this.set(13, 0);
                    this.set(14, 0);
                    this.add(2, 1);
                    break;
                default:
                    throw new IllegalStateException("Unknown periodicity type.");
            }

            return this.getTime();
        }
    }

    public static class DailyRollingFileAppender extends LoggingBuilder.FileAppender {
        static final int TOP_OF_TROUBLE = -1;
        static final int TOP_OF_MINUTE = 0;
        static final int TOP_OF_HOUR = 1;
        static final int HALF_DAY = 2;
        static final int TOP_OF_DAY = 3;
        static final int TOP_OF_WEEK = 4;
        static final int TOP_OF_MONTH = 5;
        private String datePattern = "'.'yyyy-MM-dd";
        private String scheduledFilename;
        private long nextCheck = System.currentTimeMillis() - 1L;
        Date now = new Date();
        SimpleDateFormat sdf;
        LoggingBuilder.RollingCalendar rc = new LoggingBuilder.RollingCalendar();
        final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

        public DailyRollingFileAppender() {
        }

        public void setDatePattern(String pattern) {
            this.datePattern = pattern;
        }

        public String getDatePattern() {
            return this.datePattern;
        }

        public void activateOptions() {
            super.activateOptions();
            if (this.datePattern != null && this.fileName != null) {
                this.now.setTime(System.currentTimeMillis());
                this.sdf = new SimpleDateFormat(this.datePattern);
                int type = this.computeCheckPeriod();
                this.printPeriodicity(type);
                this.rc.setType(type);
                File file = new File(this.fileName);
                this.scheduledFilename = this.fileName + this.sdf.format(new Date(file.lastModified()));
            } else {
                SysLogger.error("Either File or DatePattern options are not set for appender [" + this.name + "].");
            }

        }

        void printPeriodicity(int type) {
            switch(type) {
                case 0:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled every minute.");
                    break;
                case 1:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled on top of every hour.");
                    break;
                case 2:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled at midday and midnight.");
                    break;
                case 3:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled at midnight.");
                    break;
                case 4:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled at start of week.");
                    break;
                case 5:
                    SysLogger.debug("Appender [" + this.name + "] to be rolled at start of every month.");
                    break;
                default:
                    SysLogger.warn("Unknown periodicity for appender [" + this.name + "].");
            }

        }

        int computeCheckPeriod() {
            LoggingBuilder.RollingCalendar rollingCalendar = new LoggingBuilder.RollingCalendar(this.gmtTimeZone, Locale.getDefault());
            Date epoch = new Date(0L);
            if (this.datePattern != null) {
                for(int i = 0; i <= 5; ++i) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.datePattern);
                    simpleDateFormat.setTimeZone(this.gmtTimeZone);
                    String r0 = simpleDateFormat.format(epoch);
                    rollingCalendar.setType(i);
                    Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
                    String r1 = simpleDateFormat.format(next);
                    if (r0 != null && r1 != null && !r0.equals(r1)) {
                        return i;
                    }
                }
            }

            return -1;
        }

        void rollOver() throws IOException {
            if (this.datePattern == null) {
                this.handleError("Missing DatePattern option in rollOver().");
            } else {
                String datedFilename = this.fileName + this.sdf.format(this.now);
                if (!this.scheduledFilename.equals(datedFilename)) {
                    this.closeFile();
                    File target = new File(this.scheduledFilename);
                    if (target.exists() && !target.delete()) {
                        SysLogger.error("Failed to delete [" + this.scheduledFilename + "].");
                    }

                    File file = new File(this.fileName);
                    boolean result = file.renameTo(target);
                    if (result) {
                        SysLogger.debug(this.fileName + " -> " + this.scheduledFilename);
                    } else {
                        SysLogger.error("Failed to rename [" + this.fileName + "] to [" + this.scheduledFilename + "].");
                    }

                    try {
                        this.setFile(this.fileName, true, this.bufferedIO, this.bufferSize);
                    } catch (IOException var6) {
                        this.handleError("setFile(" + this.fileName + ", true) call failed.");
                    }

                    this.scheduledFilename = datedFilename;
                }
            }
        }

        protected void subAppend(LoggingEvent event) {
            long n = System.currentTimeMillis();
            if (n >= this.nextCheck) {
                this.now.setTime(n);
                this.nextCheck = this.rc.getNextCheckMillis(this.now);

                try {
                    this.rollOver();
                } catch (IOException var5) {
                    if (var5 instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                    }

                    SysLogger.error("rollOver() failed.", var5);
                }
            }

            super.subAppend(event);
        }
    }

    public static class RollingFileAppender extends LoggingBuilder.FileAppender {
        protected long maxFileSize = 10485760L;
        protected int maxBackupIndex = 1;
        private long nextRollover = 0L;

        public RollingFileAppender() {
        }

        public int getMaxBackupIndex() {
            return this.maxBackupIndex;
        }

        public long getMaximumFileSize() {
            return this.maxFileSize;
        }

        public void rollOver() {
            if (this.qw != null) {
                long size = ((LoggingBuilder.RollingFileAppender.CountingQuietWriter)this.qw).getCount();
                SysLogger.debug("rolling over count=" + size);
                this.nextRollover = size + this.maxFileSize;
            }

            SysLogger.debug("maxBackupIndex=" + this.maxBackupIndex);
            boolean renameSucceeded = true;
            if (this.maxBackupIndex > 0) {
                File file = new File(this.fileName + '.' + this.maxBackupIndex);
                if (file.exists()) {
                    renameSucceeded = file.delete();
                }

                File target;
                for(int i = this.maxBackupIndex - 1; i >= 1 && renameSucceeded; --i) {
                    file = new File(this.fileName + "." + i);
                    if (file.exists()) {
                        target = new File(this.fileName + '.' + (i + 1));
                        SysLogger.debug("Renaming file " + file + " to " + target);
                        renameSucceeded = file.renameTo(target);
                    }
                }

                if (renameSucceeded) {
                    target = new File(this.fileName + "." + 1);
                    this.closeFile();
                    file = new File(this.fileName);
                    SysLogger.debug("Renaming file " + file + " to " + target);
                    renameSucceeded = file.renameTo(target);
                    if (!renameSucceeded) {
                        try {
                            this.setFile(this.fileName, true, this.bufferedIO, this.bufferSize);
                        } catch (IOException var6) {
                            if (var6 instanceof InterruptedIOException) {
                                Thread.currentThread().interrupt();
                            }

                            SysLogger.error("setFile(" + this.fileName + ", true) call failed.", var6);
                        }
                    }
                }
            }

            if (renameSucceeded) {
                try {
                    this.setFile(this.fileName, false, this.bufferedIO, this.bufferSize);
                    this.nextRollover = 0L;
                } catch (IOException var5) {
                    if (var5 instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                    }

                    SysLogger.error("setFile(" + this.fileName + ", false) call failed.", var5);
                }
            }

        }

        public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
            super.setFile(fileName, append, this.bufferedIO, this.bufferSize);
            if (append) {
                File f = new File(fileName);
                ((LoggingBuilder.RollingFileAppender.CountingQuietWriter)this.qw).setCount(f.length());
            }

        }

        public void setMaxBackupIndex(int maxBackups) {
            this.maxBackupIndex = maxBackups;
        }

        public void setMaximumFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        protected void setQWForFiles(Writer writer) {
            this.qw = new LoggingBuilder.RollingFileAppender.CountingQuietWriter(writer, this);
        }

        protected void subAppend(LoggingEvent event) {
            super.subAppend(event);
            if (this.fileName != null && this.qw != null) {
                long size = ((LoggingBuilder.RollingFileAppender.CountingQuietWriter)this.qw).getCount();
                if (size >= this.maxFileSize && size >= this.nextRollover) {
                    this.rollOver();
                }
            }

        }

        protected class CountingQuietWriter extends LoggingBuilder.QuietWriter {
            protected long count;

            public CountingQuietWriter(Writer writer, Appender appender) {
                super(writer, appender);
            }

            public void write(String string) {
                try {
                    this.out.write(string);
                    this.count += (long)string.length();
                } catch (IOException var3) {
                    this.appender.handleError("Write failure.", var3, 1);
                }

            }

            public long getCount() {
                return this.count;
            }

            public void setCount(long count) {
                this.count = count;
            }
        }
    }

    public static class FileAppender extends LoggingBuilder.WriterAppender {
        protected boolean fileAppend = true;
        protected String fileName = null;
        protected boolean bufferedIO = false;
        protected int bufferSize = 8192;

        public FileAppender() {
        }

        public FileAppender(Layout layout, String filename, boolean append) throws IOException {
            this.layout = layout;
            this.setFile(filename, append, false, this.bufferSize);
        }

        public void setFile(String file) {
            this.fileName = file.trim();
        }

        public boolean getAppend() {
            return this.fileAppend;
        }

        public String getFile() {
            return this.fileName;
        }

        public void activateOptions() {
            if (this.fileName != null) {
                try {
                    this.setFile(this.fileName, this.fileAppend, this.bufferedIO, this.bufferSize);
                } catch (IOException var2) {
                    this.handleError("setFile(" + this.fileName + "," + this.fileAppend + ") call failed.", var2, 4);
                }
            } else {
                SysLogger.warn("File option not set for appender [" + this.name + "].");
                SysLogger.warn("Are you using FileAppender instead of ConsoleAppender?");
            }

        }

        protected void closeFile() {
            if (this.qw != null) {
                try {
                    this.qw.close();
                } catch (IOException var2) {
                    if (var2 instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                    }

                    SysLogger.error("Could not close " + this.qw, var2);
                }
            }

        }

        public boolean getBufferedIO() {
            return this.bufferedIO;
        }

        public int getBufferSize() {
            return this.bufferSize;
        }

        public void setAppend(boolean flag) {
            this.fileAppend = flag;
        }

        public void setBufferedIO(boolean bufferedIO) {
            this.bufferedIO = bufferedIO;
            if (bufferedIO) {
                this.immediateFlush = false;
            }

        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
            SysLogger.debug("setFile called: " + fileName + ", " + append);
            if (bufferedIO) {
                this.setImmediateFlush(false);
            }

            this.reset();

            FileOutputStream ostream;
            try {
                ostream = new FileOutputStream(fileName, append);
            } catch (FileNotFoundException var9) {
                label29: {
                    String parentName = (new File(fileName)).getParent();
                    if (parentName != null) {
                        File parentDir = new File(parentName);
                        if (!parentDir.exists() && parentDir.mkdirs()) {
                            ostream = new FileOutputStream(fileName, append);
                            break label29;
                        }

                        throw var9;
                    }

                    throw var9;
                }
            }

            Writer fw = this.createWriter(ostream);
            if (bufferedIO) {
                fw = new BufferedWriter((Writer)fw, bufferSize);
            }

            this.setQWForFiles((Writer)fw);
            this.fileName = fileName;
            this.fileAppend = append;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.writeHeader();
            SysLogger.debug("setFile ended");
        }

        protected void setQWForFiles(Writer writer) {
            this.qw = new LoggingBuilder.QuietWriter(writer, this);
        }

        protected void reset() {
            this.closeFile();
            this.fileName = null;
            super.reset();
        }
    }

    public static class WriterAppender extends Appender {
        protected boolean immediateFlush = true;
        protected String encoding;
        protected LoggingBuilder.QuietWriter qw;

        public WriterAppender() {
        }

        public void setImmediateFlush(boolean value) {
            this.immediateFlush = value;
        }

        public boolean getImmediateFlush() {
            return this.immediateFlush;
        }

        public void activateOptions() {
        }

        public void append(LoggingEvent event) {
            if (this.checkEntryConditions()) {
                this.subAppend(event);
            }
        }

        protected boolean checkEntryConditions() {
            if (this.closed) {
                SysLogger.warn("Not allowed to write to a closed appender.");
                return false;
            } else if (this.qw == null) {
                this.handleError("No output stream or file set for the appender named [" + this.name + "].");
                return false;
            } else if (this.layout == null) {
                this.handleError("No layout set for the appender named [" + this.name + "].");
                return false;
            } else {
                return true;
            }
        }

        public synchronized void close() {
            if (!this.closed) {
                this.closed = true;
                this.writeFooter();
                this.reset();
            }
        }

        protected void closeWriter() {
            if (this.qw != null) {
                try {
                    this.qw.close();
                } catch (IOException var2) {
                    this.handleError("Could not close " + this.qw, var2, 3);
                }
            }

        }

        protected OutputStreamWriter createWriter(OutputStream os) {
            OutputStreamWriter retval = null;
            String enc = this.getEncoding();
            if (enc != null) {
                try {
                    retval = new OutputStreamWriter(os, enc);
                } catch (IOException var5) {
                    SysLogger.warn("Error initializing output writer.");
                    SysLogger.warn("Unsupported encoding?");
                }
            }

            if (retval == null) {
                retval = new OutputStreamWriter(os);
            }

            return retval;
        }

        public String getEncoding() {
            return this.encoding;
        }

        public void setEncoding(String value) {
            this.encoding = value;
        }

        public synchronized void setWriter(Writer writer) {
            this.reset();
            this.qw = new LoggingBuilder.QuietWriter(writer, this);
            this.writeHeader();
        }

        protected void subAppend(LoggingEvent event) {
            this.qw.write(this.layout.format(event));
            if (this.layout.ignoresThrowable()) {
                String[] s = event.getThrowableStr();
                if (s != null) {
                    String[] var3 = s;
                    int var4 = s.length;

                    for(int var5 = 0; var5 < var4; ++var5) {
                        String s1 = var3[var5];
                        this.qw.write(s1);
                        this.qw.write(LINE_SEP);
                    }
                }
            }

            if (this.shouldFlush(event)) {
                this.qw.flush();
            }

        }

        protected void reset() {
            this.closeWriter();
            this.qw = null;
        }

        protected void writeFooter() {
            if (this.layout != null) {
                String f = this.layout.getFooter();
                if (f != null && this.qw != null) {
                    this.qw.write(f);
                    this.qw.flush();
                }
            }

        }

        protected void writeHeader() {
            if (this.layout != null) {
                String h = this.layout.getHeader();
                if (h != null && this.qw != null) {
                    this.qw.write(h);
                }
            }

        }

        protected boolean shouldFlush(LoggingEvent event) {
            return event != null && this.immediateFlush;
        }
    }

    private static class QuietWriter extends FilterWriter {
        protected Appender appender;

        public QuietWriter(Writer writer, Appender appender) {
            super(writer);
            this.appender = appender;
        }

        public void write(String string) {
            if (string != null) {
                try {
                    this.out.write(string);
                } catch (Exception var3) {
                    this.appender.handleError("Failed to write [" + string + "].", var3, 1);
                }
            }

        }

        public void flush() {
            try {
                this.out.flush();
            } catch (Exception var2) {
                this.appender.handleError("Failed to flush writer,", var2, 2);
            }

        }
    }

    public static class AsyncAppender extends Appender implements Appender.AppenderPipeline {
        public static final int DEFAULT_BUFFER_SIZE = 128;
        private final List<LoggingEvent> buffer = new ArrayList();
        private final Map<String, LoggingBuilder.AsyncAppender.DiscardSummary> discardMap = new HashMap();
        private int bufferSize = 128;
        private final AppenderPipelineImpl appenderPipeline = new AppenderPipelineImpl();
        private final Thread dispatcher;
        private boolean blocking = true;

        public AsyncAppender() {
            this.dispatcher = new Thread(new LoggingBuilder.AsyncAppender.Dispatcher(this, this.buffer, this.discardMap, this.appenderPipeline));
            this.dispatcher.setDaemon(true);
            this.dispatcher.setName("AsyncAppender-Dispatcher-" + this.dispatcher.getName());
            this.dispatcher.start();
        }

        public void addAppender(Appender newAppender) {
            synchronized(this.appenderPipeline) {
                this.appenderPipeline.addAppender(newAppender);
            }
        }

        public void append(LoggingEvent event) {
            if (this.dispatcher != null && this.dispatcher.isAlive() && this.bufferSize > 0) {
                event.getThreadName();
                event.getRenderedMessage();
                synchronized(this.buffer) {
                    while(true) {
                        int previousSize = this.buffer.size();
                        if (previousSize < this.bufferSize) {
                            this.buffer.add(event);
                            if (previousSize == 0) {
                                this.buffer.notifyAll();
                            }
                            break;
                        }

                        boolean discard = true;
                        if (this.blocking && !Thread.interrupted() && Thread.currentThread() != this.dispatcher) {
                            try {
                                this.buffer.wait();
                                discard = false;
                            } catch (InterruptedException var8) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        if (discard) {
                            String loggerName = event.getLoggerName();
                            LoggingBuilder.AsyncAppender.DiscardSummary summary = (LoggingBuilder.AsyncAppender.DiscardSummary)this.discardMap.get(loggerName);
                            if (summary == null) {
                                summary = new LoggingBuilder.AsyncAppender.DiscardSummary(event);
                                this.discardMap.put(loggerName, summary);
                            } else {
                                summary.add(event);
                            }
                            break;
                        }
                    }

                }
            } else {
                synchronized(this.appenderPipeline) {
                    this.appenderPipeline.appendLoopOnAppenders(event);
                }
            }
        }

        public void close() {
            synchronized(this.buffer) {
                this.closed = true;
                this.buffer.notifyAll();
            }

            try {
                this.dispatcher.join();
            } catch (InterruptedException var5) {
                Thread.currentThread().interrupt();
                SysLogger.error("Got an InterruptedException while waiting for the dispatcher to finish.", var5);
            }

            synchronized(this.appenderPipeline) {
                Enumeration iter = this.appenderPipeline.getAllAppenders();
                if (iter != null) {
                    while(iter.hasMoreElements()) {
                        Object next = iter.nextElement();
                        if (next instanceof Appender) {
                            ((Appender)next).close();
                        }
                    }
                }

            }
        }

        public Enumeration getAllAppenders() {
            synchronized(this.appenderPipeline) {
                return this.appenderPipeline.getAllAppenders();
            }
        }

        public Appender getAppender(String name) {
            synchronized(this.appenderPipeline) {
                return this.appenderPipeline.getAppender(name);
            }
        }

        public boolean isAttached(Appender appender) {
            synchronized(this.appenderPipeline) {
                return this.appenderPipeline.isAttached(appender);
            }
        }

        public void removeAllAppenders() {
            synchronized(this.appenderPipeline) {
                this.appenderPipeline.removeAllAppenders();
            }
        }

        public void removeAppender(Appender appender) {
            synchronized(this.appenderPipeline) {
                this.appenderPipeline.removeAppender(appender);
            }
        }

        public void removeAppender(String name) {
            synchronized(this.appenderPipeline) {
                this.appenderPipeline.removeAppender(name);
            }
        }

        public void setBufferSize(int size) {
            if (size < 0) {
                throw new NegativeArraySizeException("size");
            } else {
                synchronized(this.buffer) {
                    this.bufferSize = size < 1 ? 1 : size;
                    this.buffer.notifyAll();
                }
            }
        }

        public int getBufferSize() {
            return this.bufferSize;
        }

        public void setBlocking(boolean value) {
            synchronized(this.buffer) {
                this.blocking = value;
                this.buffer.notifyAll();
            }
        }

        public boolean getBlocking() {
            return this.blocking;
        }

        private class Dispatcher implements Runnable {
            private final LoggingBuilder.AsyncAppender parent;
            private final List<LoggingEvent> buffer;
            private final Map<String, LoggingBuilder.AsyncAppender.DiscardSummary> discardMap;
            private final AppenderPipelineImpl appenderPipeline;

            public Dispatcher(LoggingBuilder.AsyncAppender parent, List<LoggingEvent> buffer, Map<String, LoggingBuilder.AsyncAppender.DiscardSummary> discardMap, AppenderPipelineImpl appenderPipeline) {
                this.parent = parent;
                this.buffer = buffer;
                this.appenderPipeline = appenderPipeline;
                this.discardMap = discardMap;
            }

            public void run() {
                boolean isActive = true;

                try {
                    while(isActive) {
                        LoggingEvent[] events = null;
                        int bufferSize;
                        int index;
                        synchronized(this.buffer) {
                            bufferSize = this.buffer.size();

                            for(isActive = !this.parent.closed; bufferSize == 0 && isActive; isActive = !this.parent.closed) {
                                this.buffer.wait();
                                bufferSize = this.buffer.size();
                            }

                            if (bufferSize > 0) {
                                events = new LoggingEvent[bufferSize + this.discardMap.size()];
                                this.buffer.toArray(events);
                                index = bufferSize;
                                Collection<LoggingBuilder.AsyncAppender.DiscardSummary> values = this.discardMap.values();

                                LoggingBuilder.AsyncAppender.DiscardSummary value;
                                for(Iterator var7 = values.iterator(); var7.hasNext(); events[index++] = value.createEvent()) {
                                    value = (LoggingBuilder.AsyncAppender.DiscardSummary)var7.next();
                                }

                                this.buffer.clear();
                                this.discardMap.clear();
                                this.buffer.notifyAll();
                            }
                        }

                        if (events != null) {
                            LoggingEvent[] var3 = events;
                            bufferSize = events.length;

                            for(index = 0; index < bufferSize; ++index) {
                                LoggingEvent event = var3[index];
                                synchronized(this.appenderPipeline) {
                                    this.appenderPipeline.appendLoopOnAppenders(event);
                                }
                            }
                        }
                    }
                } catch (InterruptedException var13) {
                    Thread.currentThread().interrupt();
                }

            }
        }

        private final class DiscardSummary {
            private LoggingEvent maxEvent;
            private int count;

            public DiscardSummary(LoggingEvent event) {
                this.maxEvent = event;
                this.count = 1;
            }

            public void add(LoggingEvent event) {
                if (event.getLevel().toInt() > this.maxEvent.getLevel().toInt()) {
                    this.maxEvent = event;
                }

                ++this.count;
            }

            public LoggingEvent createEvent() {
                String msg = MessageFormat.format("Discarded {0} messages due to full event buffer including: {1}", this.count, this.maxEvent.getMessage());
                return new LoggingEvent("AsyncAppender.DONT_REPORT_LOCATION", Logger.getLogger(this.maxEvent.getLoggerName()), this.maxEvent.getLevel(), msg, (Throwable)null);
            }
        }
    }

    public static class AppenderBuilder {
        private LoggingBuilder.AsyncAppender asyncAppender;
        private Appender appender;

        private AppenderBuilder() {
            this.appender = null;
        }

        public LoggingBuilder.AppenderBuilder withLayout(Layout layout) {
            this.appender.setLayout(layout);
            return this;
        }

        public LoggingBuilder.AppenderBuilder withName(String name) {
            this.appender.setName(name);
            return this;
        }

        public LoggingBuilder.AppenderBuilder withConsoleAppender(String target) {
            LoggingBuilder.ConsoleAppender consoleAppender = new LoggingBuilder.ConsoleAppender();
            consoleAppender.setTarget(target);
            consoleAppender.activateOptions();
            this.appender = consoleAppender;
            return this;
        }

        public LoggingBuilder.AppenderBuilder withFileAppender(String file) {
            LoggingBuilder.FileAppender appender = new LoggingBuilder.FileAppender();
            appender.setFile(file);
            appender.setAppend(true);
            appender.setBufferedIO(false);
            appender.setEncoding(LoggingBuilder.ENCODING);
            appender.setImmediateFlush(true);
            appender.activateOptions();
            this.appender = appender;
            return this;
        }

        public LoggingBuilder.AppenderBuilder withRollingFileAppender(String file, String maxFileSize, int maxFileIndex) {
            LoggingBuilder.RollingFileAppender appender = new LoggingBuilder.RollingFileAppender();
            appender.setFile(file);
            appender.setAppend(true);
            appender.setBufferedIO(false);
            appender.setEncoding(LoggingBuilder.ENCODING);
            appender.setImmediateFlush(true);
            appender.setMaximumFileSize((long)Integer.parseInt(maxFileSize));
            appender.setMaxBackupIndex(maxFileIndex);
            appender.activateOptions();
            this.appender = appender;
            return this;
        }

        public LoggingBuilder.AppenderBuilder withDailyFileRollingAppender(String file, String datePattern) {
            LoggingBuilder.DailyRollingFileAppender appender = new LoggingBuilder.DailyRollingFileAppender();
            appender.setFile(file);
            appender.setAppend(true);
            appender.setBufferedIO(false);
            appender.setEncoding(LoggingBuilder.ENCODING);
            appender.setImmediateFlush(true);
            appender.setDatePattern(datePattern);
            appender.activateOptions();
            this.appender = appender;
            return this;
        }

        public LoggingBuilder.AppenderBuilder withAsync(boolean blocking, int buffSize) {
            LoggingBuilder.AsyncAppender asyncAppender = new LoggingBuilder.AsyncAppender();
            asyncAppender.setBlocking(blocking);
            asyncAppender.setBufferSize(buffSize);
            this.asyncAppender = asyncAppender;
            return this;
        }

        public Appender build() {
            if (this.appender == null) {
                throw new RuntimeException("please specify appender first");
            } else if (this.asyncAppender != null) {
                this.asyncAppender.addAppender(this.appender);
                return this.asyncAppender;
            } else {
                return this.appender;
            }
        }
    }
}
