package org.apache.rocketmq.sdk.shade.logging.inner;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class Logger implements Appender.AppenderPipeline {
    private static final String FQCN = Logger.class.getName();
    private static final DefaultLoggerRepository REPOSITORY = new DefaultLoggerRepository(new RootLogger(Level.DEBUG));
    private String name;
    private volatile Level level;
    private volatile Logger parent;
    Appender.AppenderPipelineImpl appenderPipeline;
    private boolean additive;

    public interface LoggerRepository {
        boolean isDisabled(int i);

        void setLogLevel(Level level);

        void emitNoAppenderWarning(Logger logger);

        Level getLogLevel();

        Logger getLogger(String str);

        Logger getRootLogger();

        Logger exists(String str);

        void shutdown();

        Enumeration getCurrentLoggers();
    }

    public static LoggerRepository getRepository() {
        return REPOSITORY;
    }

    private Logger(String name) {
        this.additive = true;
        this.name = name;
    }

    public static Logger getLogger(String name) {
        return getRepository().getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return getRepository().getLogger(clazz.getName());
    }

    public static Logger getRootLogger() {
        return getRepository().getRootLogger();
    }

    @Override
    public synchronized void addAppender(Appender newAppender) {
        if (this.appenderPipeline == null) {
            this.appenderPipeline = new Appender.AppenderPipelineImpl();
        }
        this.appenderPipeline.addAppender(newAppender);
    }

    public void callAppenders(LoggingEvent event) {
        int writes = 0;

        for (Logger logger = this; logger != null; logger = logger.parent) {
            synchronized (logger) {
                if (logger.appenderPipeline != null) {
                    writes += logger.appenderPipeline.appendLoopOnAppenders(event);
                }
                if (!logger.additive) {
                    break;
                }
            }
        }

        if (writes == 0) {
            getRepository().emitNoAppenderWarning(this);
        }
    }

    synchronized void closeNestedAppenders() {
        Enumeration enumeration = getAllAppenders();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Appender a = (Appender) enumeration.nextElement();
                if (a instanceof Appender.AppenderPipeline) {
                    a.close();
                }
            }
        }
    }

    public void debug(Object message) {
        if (!getRepository().isDisabled(10000) && Level.DEBUG.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.DEBUG, message, null);
        }
    }

    public void debug(Object message, Throwable t) {
        if (!getRepository().isDisabled(10000) && Level.DEBUG.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.DEBUG, message, t);
        }
    }

    public void error(Object message) {
        if (!getRepository().isDisabled(Level.ERROR_INT) && Level.ERROR.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.ERROR, message, null);
        }
    }

    public void error(Object message, Throwable t) {
        if (!getRepository().isDisabled(Level.ERROR_INT) && Level.ERROR.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.ERROR, message, t);
        }
    }

    protected void forcedLog(String fqcn, Level level, Object message, Throwable t) {
        callAppenders(new LoggingEvent(fqcn, this, level, message, t));
    }

    @Override
    public synchronized Enumeration getAllAppenders() {
        if (this.appenderPipeline == null) {
            return null;
        }
        return this.appenderPipeline.getAllAppenders();
    }

    @Override
    public synchronized Appender getAppender(String name) {
        if (this.appenderPipeline == null || name == null) {
            return null;
        }
        return this.appenderPipeline.getAppender(name);
    }

    public Level getEffectiveLevel() {
        for (Logger c = this; c != null; c = c.parent) {
            if (c.level != null) {
                return c.level;
            }
        }
        return null;
    }

    public final String getName() {
        return this.name;
    }

    public final Level getLevel() {
        return this.level;
    }

    public void info(Object message) {
        if (!getRepository().isDisabled(Level.INFO_INT) && Level.INFO.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.INFO, message, null);
        }
    }

    public void info(Object message, Throwable t) {
        if (!getRepository().isDisabled(Level.INFO_INT) && Level.INFO.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.INFO, message, t);
        }
    }

    @Override
    public boolean isAttached(Appender appender) {
        return (appender == null || this.appenderPipeline == null || !this.appenderPipeline.isAttached(appender)) ? false : true;
    }

    @Override
    public synchronized void removeAllAppenders() {
        if (this.appenderPipeline != null) {
            this.appenderPipeline.removeAllAppenders();
            this.appenderPipeline = null;
        }
    }

    @Override
    public synchronized void removeAppender(Appender appender) {
        if (appender != null && this.appenderPipeline != null) {
            this.appenderPipeline.removeAppender(appender);
        }
    }

    @Override
    public synchronized void removeAppender(String name) {
        if (name != null && this.appenderPipeline != null) {
            this.appenderPipeline.removeAppender(name);
        }
    }

    public void setAdditivity(boolean additive) {
        this.additive = additive;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void warn(Object message) {
        if (!getRepository().isDisabled(Level.WARN_INT) && Level.WARN.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.WARN, message, null);
        }
    }

    public void warn(Object message, Throwable t) {
        if (!getRepository().isDisabled(Level.WARN_INT) && Level.WARN.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.WARN, message, t);
        }
    }

    public static class ProvisionNode extends Vector<Logger> {
        ProvisionNode(Logger logger) {
            addElement(logger);
        }
    }

    public static class DefaultLoggerRepository implements LoggerRepository {
        Logger root;
        int logLevelInt;
        Level logLevel;
        final Hashtable<CategoryKey, Object> ht = new Hashtable<>();
        boolean emittedNoAppenderWarning = false;

        public DefaultLoggerRepository(Logger root) {
            this.root = root;
            setLogLevel(Level.ALL);
        }

        @Override
        public void emitNoAppenderWarning(Logger cat) {
            if (!this.emittedNoAppenderWarning) {
                SysLogger.warn("No appenders could be found for logger (" + cat.getName() + ").");
                SysLogger.warn("Please initialize the logger system properly.");
                this.emittedNoAppenderWarning = true;
            }
        }

        @Override
        public Logger exists(String name) {
            Object o = this.ht.get(new CategoryKey(name));
            if (o instanceof Logger) {
                return (Logger) o;
            }
            return null;
        }

        @Override
        public void setLogLevel(Level l) {
            if (l != null) {
                this.logLevelInt = l.level;
                this.logLevel = l;
            }
        }

        @Override
        public Level getLogLevel() {
            return this.logLevel;
        }

        @Override
        public Logger getLogger(String name) {
            CategoryKey key = new CategoryKey(name);
            synchronized (this.ht) {
                Object o = this.ht.get(key);
                if (o == null) {
                    Logger logger = makeNewLoggerInstance(name);
                    this.ht.put(key, logger);
                    updateParents(logger);
                    return logger;
                } else if (o instanceof Logger) {
                    return (Logger) o;
                } else if (!(o instanceof ProvisionNode)) {
                    return null;
                } else {
                    Logger logger2 = makeNewLoggerInstance(name);
                    this.ht.put(key, logger2);
                    updateChildren((ProvisionNode) o, logger2);
                    updateParents(logger2);
                    return logger2;
                }
            }
        }

        public Logger makeNewLoggerInstance(String name) {
            return new Logger(name);
        }

        @Override
        public Enumeration getCurrentLoggers() {
            Vector<Logger> loggers = new Vector<>(this.ht.size());
            Enumeration elems = this.ht.elements();
            while (elems.hasMoreElements()) {
                Object o = elems.nextElement();
                if (o instanceof Logger) {
                    loggers.addElement((Logger) o);
                }
            }
            return loggers.elements();
        }

        @Override
        public Logger getRootLogger() {
            return this.root;
        }

        @Override
        public boolean isDisabled(int level) {
            return this.logLevelInt > level;
        }

        @Override
        public void shutdown() {
            Logger root = getRootLogger();
            root.closeNestedAppenders();
            synchronized (this.ht) {
                Enumeration cats = getCurrentLoggers();
                while (cats.hasMoreElements()) {
                    ((Logger) cats.nextElement()).closeNestedAppenders();
                }
                root.removeAllAppenders();
            }
        }

        private void updateParents(Logger cat) {
            String name = cat.name;
            boolean parentFound = false;
            int i = name.lastIndexOf(46, name.length() - 1);
            while (true) {
                if (i < 0) {
                    break;
                }
                CategoryKey key = new CategoryKey(name.substring(0, i));
                Object o = this.ht.get(key);
                if (o == null) {
                    this.ht.put(key, new ProvisionNode(cat));
                } else if (o instanceof Logger) {
                    parentFound = true;
                    cat.parent = (Logger) o;
                    break;
                } else if (o instanceof ProvisionNode) {
                    ((ProvisionNode) o).addElement(cat);
                } else {
                    new IllegalStateException("unexpected object type " + o.getClass() + " in ht.").printStackTrace();
                }
                i = name.lastIndexOf(46, i - 1);
            }
            if (!parentFound) {
                cat.parent = this.root;
            }
        }

        private void updateChildren(ProvisionNode pn, Logger logger) {
            int last = pn.size();
            for (int i = 0; i < last; i++) {
                Logger l = pn.elementAt(i);
                if (!l.parent.name.startsWith(logger.name)) {
                    logger.parent = l.parent;
                    l.parent = logger;
                }
            }
        }

        public class CategoryKey {
            String name;
            int hashCache;

            CategoryKey(String name) {
                this.name = name;
                this.hashCache = name.hashCode();
            }

            public final int hashCode() {
                return this.hashCache;
            }

            public final boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || !(o instanceof CategoryKey)) {
                    return false;
                }
                return this.name.equals(((CategoryKey) o).name);
            }
        }
    }

    public static class RootLogger extends Logger {
        public RootLogger(Level level) {
            super("root");
            setLevel(level);
        }
    }
}
