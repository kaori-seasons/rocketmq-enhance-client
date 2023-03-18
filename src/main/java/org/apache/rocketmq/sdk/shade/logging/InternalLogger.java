package org.apache.rocketmq.sdk.shade.logging;

public interface InternalLogger {
    String getName();

    void debug(String str);

    void debug(String str, Object obj);

    void debug(String str, Object obj, Object obj2);

    void debug(String str, Object... objArr);

    void debug(String str, Throwable th);

    void info(String str);

    void info(String str, Object obj);

    void info(String str, Object obj, Object obj2);

    void info(String str, Object... objArr);

    void info(String str, Throwable th);

    void warn(String str);

    void warn(String str, Object obj);

    void warn(String str, Object... objArr);

    void warn(String str, Object obj, Object obj2);

    void warn(String str, Throwable th);

    void error(String str);

    void error(String str, Object obj);

    void error(String str, Object obj, Object obj2);

    void error(String str, Object... objArr);

    void error(String str, Throwable th);
}
