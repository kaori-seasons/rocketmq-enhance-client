package org.apache.rocketmq.sdk.shade.logging.inner;

public class SysLogger {
    protected static boolean debugEnabled = false;
    private static boolean quietMode = false;
    private static final String PREFIX = "RocketMQLog: ";
    private static final String ERR_PREFIX = "RocketMQLog:ERROR ";
    private static final String WARN_PREFIX = "RocketMQLog:WARN ";

    public static void setInternalDebugging(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void debug(String msg) {
        if (debugEnabled && !quietMode) {
            System.out.printf("%s", PREFIX + msg);
        }
    }

    public static void debug(String msg, Throwable t) {
        if (debugEnabled && !quietMode) {
            System.out.printf("%s", PREFIX + msg);
            if (t != null) {
                t.printStackTrace(System.out);
            }
        }
    }

    public static void error(String msg) {
        if (!quietMode) {
            System.err.println(ERR_PREFIX + msg);
        }
    }

    public static void error(String msg, Throwable t) {
        if (!quietMode) {
            System.err.println(ERR_PREFIX + msg);
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    public static void setQuietMode(boolean quietMode2) {
        quietMode = quietMode2;
    }

    public static void warn(String msg) {
        if (!quietMode) {
            System.err.println(WARN_PREFIX + msg);
        }
    }

    public static void warn(String msg, Throwable t) {
        if (!quietMode) {
            System.err.println(WARN_PREFIX + msg);
            if (t != null) {
                t.printStackTrace();
            }
        }
    }
}
