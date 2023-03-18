package org.apache.rocketmq.sdk.shade.logging.inner;

import java.io.Serializable;

public class Level implements Serializable {
    transient int level;
    transient String levelStr;
    transient int syslogEquivalent;
    public static final int OFF_INT = Integer.MAX_VALUE;
    public static final int DEBUG_INT = 10000;
    public static final int ALL_INT = Integer.MIN_VALUE;
    static final long serialVersionUID = 3491141966387921974L;
    private static final String OFF_NAME = "OFF";
    public static final Level OFF = new Level(Integer.MAX_VALUE, OFF_NAME, 0);
    public static final int ERROR_INT = 40000;
    private static final String ERROR_NAME = "ERROR";
    public static final Level ERROR = new Level(ERROR_INT, ERROR_NAME, 3);
    public static final int WARN_INT = 30000;
    private static final String WARN_NAME = "WARN";
    public static final Level WARN = new Level(WARN_INT, WARN_NAME, 4);
    public static final int INFO_INT = 20000;
    private static final String INFO_NAME = "INFO";
    public static final Level INFO = new Level(INFO_INT, INFO_NAME, 6);
    private static final String DEBUG_NAME = "DEBUG";
    public static final Level DEBUG = new Level(10000, DEBUG_NAME, 7);
    private static final String ALL_NAME = "ALL";
    public static final Level ALL = new Level(Integer.MIN_VALUE, ALL_NAME, 7);

    protected Level(int level, String levelStr, int syslogEquivalent) {
        this.level = level;
        this.levelStr = levelStr;
        this.syslogEquivalent = syslogEquivalent;
    }

    public static Level toLevel(String sArg) {
        return toLevel(sArg, DEBUG);
    }

    public static Level toLevel(int val) {
        return toLevel(val, DEBUG);
    }

    public static Level toLevel(int val, Level defaultLevel) {
        switch (val) {
            case Integer.MIN_VALUE:
                return ALL;
            case 10000:
                return DEBUG;
            case INFO_INT :
                return INFO;
            case WARN_INT :
                return WARN;
            case ERROR_INT :
                return ERROR;
            case Integer.MAX_VALUE:
                return OFF;
            default:
                return defaultLevel;
        }
    }

    public static Level toLevel(String sArg, Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }
        String s = sArg.toUpperCase();
        if (s.equals(ALL_NAME)) {
            return ALL;
        }
        if (s.equals(DEBUG_NAME)) {
            return DEBUG;
        }
        if (s.equals(INFO_NAME)) {
            return INFO;
        }
        if (s.equals(WARN_NAME)) {
            return WARN;
        }
        if (s.equals(ERROR_NAME)) {
            return ERROR;
        }
        if (s.equals(OFF_NAME)) {
            return OFF;
        }
        if (s.equals(INFO_NAME)) {
            return INFO;
        }
        return defaultLevel;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Level) && this.level == ((Level) o).level;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * this.level) + (this.levelStr != null ? this.levelStr.hashCode() : 0))) + this.syslogEquivalent;
    }

    public boolean isGreaterOrEqual(Level r) {
        return this.level >= r.level;
    }

    @Override
    public final String toString() {
        return this.levelStr;
    }

    public final int toInt() {
        return this.level;
    }
}
